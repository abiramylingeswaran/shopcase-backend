package com.shop.whatsapp;

import com.shop.config.WhatsAppProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts {@code whatsapp-service} ({@code npm start} on port 3001) as a child process
 * when {@code whatsapp.provider=node} and {@code whatsapp.managed-node=true}.
 * Stops the process on JVM / Spring shutdown.
 *
 * <p>If something is already bound to :3001 but WhatsApp is unhealthy
 * (Chrome missing / initialize failed / stuck offline), that process is killed
 * and a fresh managed Node is started.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNodeProcessManager {

    private final WhatsAppProperties props;

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(4))
            .build();

    private final AtomicBoolean startedByUs = new AtomicBoolean(false);
    private volatile Process process;

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void onReady() {
        if (!shouldManage()) {
            log.info("WhatsApp Node process manager idle (provider={}, managedNode={})",
                    props.getProvider(), props.isManagedNode());
            return;
        }

        try {
            Path serviceDir = resolveServiceDir();
            if (serviceDir == null) {
                log.error(
                        "WhatsApp service folder not found. Set WHATSAPP_SERVICE_DIR to the absolute "
                                + "path of whatsapp-service (must contain package.json). App continues without WhatsApp."
                );
                return;
            }

            if (!Files.isRegularFile(serviceDir.resolve("package.json"))) {
                log.error("No package.json in {} — cannot start WhatsApp Node. App continues.", serviceDir);
                return;
            }

            if (!npmAvailable()) {
                log.error(
                        "npm/node not found on PATH. Install Node.js LTS, then restart Spring Boot. "
                                + "App continues without auto-started WhatsApp."
                );
                return;
            }

            // Adopt only a healthy existing Node; otherwise free the port and start ours.
            if (isNodeHttpUp()) {
                NodeHealth health = probeNodeHealth();
                if (health.healthy()) {
                    log.info(
                            "WhatsApp Node already healthy at {} (state={}) — reusing it",
                            props.getApiUrl(),
                            health.state()
                    );
                    return;
                }
                log.warn(
                        "WhatsApp Node on {} is unhealthy (state={}, detail={}). "
                                + "Stopping it and starting a managed process…",
                        props.getApiUrl(),
                        health.state(),
                        health.detail()
                );
                freeWhatsAppPort();
                sleepQuiet(1500);
            }

            if (props.isAutoNpmInstall() && !Files.isDirectory(serviceDir.resolve("node_modules"))) {
                log.info("whatsapp-service/node_modules missing — running npm install in {}", serviceDir);
                if (!runNpmInstall(serviceDir)) {
                    log.error("npm install failed for {}. Run it manually once. App continues.", serviceDir);
                    return;
                }
            }

            startProcess(serviceDir);
            waitUntilHttpUp();
        } catch (Exception e) {
            log.error("Failed to auto-start WhatsApp Node (app continues): {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        Process p = process;
        if (p == null || !startedByUs.get()) {
            return;
        }
        log.info("Stopping managed WhatsApp Node process…");
        try {
            p.destroy();
            if (!p.waitFor(8, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                p.waitFor(3, TimeUnit.SECONDS);
            }
            log.info("WhatsApp Node process stopped (exit={})", p.exitValue());
        } catch (Exception e) {
            log.warn("Error stopping WhatsApp Node: {}", e.getMessage());
            try {
                p.destroyForcibly();
            } catch (Exception ignored) {
                // ignore
            }
        } finally {
            process = null;
            startedByUs.set(false);
        }
    }

    private boolean shouldManage() {
        if (!props.isManagedNode()) {
            return false;
        }
        String provider = props.getProvider();
        return provider == null || "node".equalsIgnoreCase(provider.trim());
    }

    private Path resolveServiceDir() {
        if (StringUtils.hasText(props.getServiceDir())) {
            Path configured = Path.of(props.getServiceDir().trim()).toAbsolutePath().normalize();
            if (Files.isDirectory(configured)) {
                return configured;
            }
            log.warn("Configured whatsapp.service-dir does not exist: {}", configured);
        }

        Path cwd = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve("whatsapp-service"));
        candidates.add(cwd.resolve("..").resolve("whatsapp-service").normalize());
        if (cwd.getParent() != null) {
            candidates.add(cwd.getParent().resolve("whatsapp-service"));
        }
        // When running a packaged jar: <deploy>/app.jar + <deploy>/whatsapp-service/
        try {
            Path codeSource = Path.of(
                    WhatsAppNodeProcessManager.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).toAbsolutePath().normalize();
            Path jarDir = Files.isRegularFile(codeSource) ? codeSource.getParent() : codeSource;
            if (jarDir != null) {
                candidates.add(jarDir.resolve("whatsapp-service"));
                candidates.add(jarDir.resolve("..").resolve("whatsapp-service").normalize());
            }
        } catch (Exception e) {
            log.debug("Could not resolve path from code source: {}", e.getMessage());
        }

        for (Path candidate : candidates) {
            Path abs = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(abs) && Files.isRegularFile(abs.resolve("package.json"))) {
                log.info("Resolved WhatsApp service dir: {}", abs);
                return abs;
            }
        }
        return null;
    }

    private int resolveServerPort() {
        String port = System.getProperty("server.port");
        if (!StringUtils.hasText(port)) {
            port = System.getenv("SERVER_PORT");
        }
        if (!StringUtils.hasText(port)) {
            return 8087;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return 8087;
        }
    }

    private boolean npmAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(npmCommand(), "-v");
            pb.redirectErrorStream(true);
            Process check = pb.start();
            boolean finished = check.waitFor(10, TimeUnit.SECONDS);
            return finished && check.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean runNpmInstall(Path serviceDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(npmCommand(), "install");
            pb.directory(serviceDir.toFile());
            pb.redirectErrorStream(true);
            applyPuppeteerEnv(pb, serviceDir);
            Process install = pb.start();
            pipeOutput(install.getInputStream(), "whatsapp-npm");
            boolean finished = install.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                install.destroyForcibly();
                return false;
            }
            return install.exitValue() == 0;
        } catch (Exception e) {
            log.error("npm install error: {}", e.getMessage());
            return false;
        }
    }

    private void startProcess(Path serviceDir) throws IOException {
        // Prefer `node index.js` over `npm start` (avoids npm env quirks / slower spawn)
        List<String> cmd = new ArrayList<>();
        cmd.add(nodeCommand());
        cmd.add("index.js");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(serviceDir.toFile());
        pb.redirectErrorStream(true);
        pb.environment().putIfAbsent("FORCE_COLOR", "0");
        applyPuppeteerEnv(pb, serviceDir);
        // Node bot callbacks → this Spring instance (same machine when hosting)
        String springBot = System.getenv("SPRING_WHATSAPP_URL");
        if (!StringUtils.hasText(springBot)) {
            springBot = "http://127.0.0.1:" + resolveServerPort() + "/whatsapp";
        }
        pb.environment().put("SPRING_WHATSAPP_URL", springBot);
        pb.environment().put("PORT", String.valueOf(extractPort(props.getApiUrl(), 3001)));

        log.info("Starting WhatsApp Node: {} in {} (SPRING_WHATSAPP_URL={})",
                String.join(" ", cmd), serviceDir, springBot);
        log.info("PUPPETEER_EXECUTABLE_PATH={}", pb.environment().get("PUPPETEER_EXECUTABLE_PATH"));
        Process p = pb.start();
        process = p;
        startedByUs.set(true);
        pipeOutput(p.getInputStream(), "whatsapp-node");

        sleepQuiet(800);
        if (!p.isAlive()) {
            startedByUs.set(false);
            process = null;
            throw new IllegalStateException(
                    "WhatsApp Node exited immediately (exit=" + p.exitValue()
                            + "). Is port 3001 already in use by another process?"
            );
        }
    }

    /**
     * Force Puppeteer to use a stable cache under whatsapp-service (not Cursor sandbox temp).
     */
    private void applyPuppeteerEnv(ProcessBuilder pb, Path serviceDir) {
        Path cache = serviceDir.resolve(".cache").resolve("puppeteer").toAbsolutePath().normalize();
        try {
            Files.createDirectories(cache);
        } catch (IOException e) {
            log.warn("Could not create puppeteer cache dir {}: {}", cache, e.getMessage());
        }
        Map<String, String> env = pb.environment();
        env.put("PUPPETEER_CACHE_DIR", cache.toString());
        env.put("PUPPETEER_DOWNLOAD_PATH", cache.toString());
        // Prefer system Chrome if present (Windows)
        String chrome = findSystemChrome();
        if (chrome != null) {
            env.put("PUPPETEER_EXECUTABLE_PATH", chrome);
            env.put("CHROME_PATH", chrome);
            log.info("Using system Chrome for WhatsApp: {}", chrome);
        }
    }

    private static String findSystemChrome() {
        String[] candidates = {
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("LOCALAPPDATA") != null
                        ? System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe"
                        : null,
                "/usr/bin/google-chrome",
                "/usr/bin/chromium-browser",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
        };
        for (String c : candidates) {
            if (c != null && Files.isRegularFile(Path.of(c))) {
                return c;
            }
        }
        return null;
    }

    private void waitUntilHttpUp() {
        int timeout = Math.max(10, props.getStartupTimeoutSeconds());
        long deadline = System.currentTimeMillis() + timeout * 1000L;
        log.info("Waiting up to {}s for WhatsApp Node HTTP at {}", timeout, props.getApiUrl());

        while (System.currentTimeMillis() < deadline) {
            Process p = process;
            if (p != null && !p.isAlive()) {
                log.error(
                        "WhatsApp Node died while starting (exit={}). Check logs above / port 3001. App continues.",
                        p.exitValue()
                );
                startedByUs.set(false);
                process = null;
                return;
            }
            if (isNodeHttpUp()) {
                log.info("WhatsApp Node is up at {}", props.getApiUrl());
                return;
            }
            sleepQuiet(1000);
        }
        log.error(
                "WhatsApp Node did not become ready within {}s at {}. "
                        + "Open Admin → WhatsApp later; scan QR if needed. App continues.",
                timeout,
                props.getApiUrl()
        );
    }

    private boolean isNodeHttpUp() {
        return probeNodeHealth().httpUp();
    }

    @SuppressWarnings("unchecked")
    private NodeHealth probeNodeHealth() {
        String base = props.getApiUrl();
        if (!StringUtils.hasText(base)) {
            return NodeHealth.down();
        }
        String url = base.replaceAll("/$", "") + "/status";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return NodeHealth.down();
            }
            Map<String, Object> body = response.getBody();
            String state = String.valueOf(body.getOrDefault("state", "offline")).toLowerCase(Locale.ROOT);
            String detail = String.valueOf(body.getOrDefault("statusMessage", ""));
            boolean ready = Boolean.TRUE.equals(body.get("ready"));

            // Healthy enough to keep: connected, or waiting for QR scan
            boolean healthy = ready
                    || "connected".equals(state)
                    || "qr_pending".equals(state)
                    || detail.toLowerCase(Locale.ROOT).contains("scan");

            // Explicitly unhealthy: chrome / puppeteer / initialize failures
            String d = detail.toLowerCase(Locale.ROOT);
            if (d.contains("could not find chrome")
                    || d.contains("initialize failed")
                    || d.contains("puppeteer")
                    || d.contains("browser")) {
                healthy = false;
            }

            return new NodeHealth(true, healthy, state, detail);
        } catch (Exception e) {
            return NodeHealth.down();
        }
    }

    private void freeWhatsAppPort() {
        int port = extractPort(props.getApiUrl(), 3001);
        log.warn("Freeing WhatsApp port {}…", port);
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder(
                        "cmd.exe", "/c",
                        "for /f \"tokens=5\" %a in ('netstat -ano ^| findstr :" + port
                                + " ^| findstr LISTENING') do taskkill /F /PID %a"
                );
                pb.redirectErrorStream(true);
                Process kill = pb.start();
                kill.waitFor(15, TimeUnit.SECONDS);
            } else {
                ProcessBuilder pb = new ProcessBuilder(
                        "bash", "-lc",
                        "fuser -k " + port + "/tcp || true"
                );
                pb.redirectErrorStream(true);
                Process kill = pb.start();
                kill.waitFor(15, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Could not free port {}: {}", port, e.getMessage());
        }
    }

    private static int extractPort(String apiUrl, int fallback) {
        try {
            if (!StringUtils.hasText(apiUrl)) {
                return fallback;
            }
            String after = apiUrl.replaceFirst("^https?://", "");
            int slash = after.indexOf('/');
            String hostPort = slash >= 0 ? after.substring(0, slash) : after;
            int colon = hostPort.lastIndexOf(':');
            if (colon >= 0) {
                return Integer.parseInt(hostPort.substring(colon + 1));
            }
        } catch (Exception ignored) {
            // fallback
        }
        return fallback;
    }

    private static String npmCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "npm.cmd" : "npm";
    }

    private static String nodeCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "node.exe" : "node";
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void pipeOutput(InputStream in, String prefix) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[{}] {}", prefix, line);
                }
            } catch (IOException e) {
                log.debug("[{}] stream closed: {}", prefix, e.getMessage());
            }
        }, prefix + "-log");
        t.setDaemon(true);
        t.start();
    }

    private record NodeHealth(boolean httpUp, boolean healthy, String state, String detail) {
        static NodeHealth down() {
            return new NodeHealth(false, false, "offline", "unreachable");
        }
    }
}
