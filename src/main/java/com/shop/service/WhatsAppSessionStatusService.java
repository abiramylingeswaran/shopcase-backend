package com.shop.service;

import com.shop.config.WhatsAppProperties;
import com.shop.dto.WhatsAppSessionStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Proxies Node WhatsApp GET /status + GET /qr + POST /logout for the admin panel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppSessionStatusService {

    private final WhatsAppProperties props;

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();

    public WhatsAppSessionStatusResponse getStatus() {
        String base = props.getApiUrl();
        if (!StringUtils.hasText(base)) {
            return WhatsAppSessionStatusResponse.builder()
                    .state("offline")
                    .ready(false)
                    .nodeReachable(false)
                    .statusMessage("WHATSUP_URL is not configured")
                    .build();
        }

        String root = base.replaceAll("/$", "");
        try {
            Map<?, ?> statusBody = getJson(root + "/status");
            if (statusBody == null) {
                return WhatsAppSessionStatusResponse.builder()
                        .state("offline")
                        .ready(false)
                        .nodeReachable(true)
                        .statusMessage("Empty status from WhatsApp service")
                        .build();
            }

            String state = asString(statusBody.get("state"));
            if (!StringUtils.hasText(state)) {
                Object connectedFlag = statusBody.get("connected");
                boolean connected = Boolean.TRUE.equals(connectedFlag)
                        || "true".equalsIgnoreCase(String.valueOf(connectedFlag));
                Object readyFlag = statusBody.get("ready");
                boolean ready = Boolean.TRUE.equals(readyFlag)
                        || "true".equalsIgnoreCase(String.valueOf(readyFlag));
                state = (connected || ready) ? "connected" : "offline";
            }

            String qrDataUrl = asString(statusBody.get("qrDataUrl"));
            String qrUpdatedAt = asString(statusBody.get("qrUpdatedAt"));

            if (!"connected".equalsIgnoreCase(state)) {
                try {
                    Map<?, ?> qrBody = getJson(root + "/qr");
                    if (qrBody != null) {
                        String fromQr = asString(qrBody.get("qrDataUrl"));
                        if (StringUtils.hasText(fromQr)) {
                            qrDataUrl = fromQr;
                            qrUpdatedAt = asString(qrBody.get("qrUpdatedAt"));
                            state = "qr_pending";
                        }
                    }
                } catch (Exception qrEx) {
                    log.debug("WhatsApp /qr: {}", qrEx.getMessage());
                }
            }

            boolean connected = "connected".equalsIgnoreCase(state);
            return WhatsAppSessionStatusResponse.builder()
                    .state(state.toLowerCase())
                    .ready(connected)
                    .statusMessage(asString(statusBody.get("statusMessage")))
                    .qrDataUrl(connected ? null : qrDataUrl)
                    .qrUpdatedAt(qrUpdatedAt)
                    .linkedNumber(asString(statusBody.get("linkedNumber")))
                    .nodeReachable(true)
                    .build();
        } catch (Exception e) {
            log.warn("WhatsApp Node unreachable: {}", e.getMessage());
            return WhatsAppSessionStatusResponse.builder()
                    .state("offline")
                    .ready(false)
                    .nodeReachable(false)
                    .statusMessage("WhatsApp service offline. Start whatsapp-service (npm start) on "
                            + props.getApiUrl())
                    .build();
        }
    }

    /**
     * Logout the linked admin WhatsApp and force a new QR so a different number can be scanned.
     */
    @SuppressWarnings("rawtypes")
    public Map<String, Object> logoutAndRequestNewQr() {
        String base = props.getApiUrl();
        if (!StringUtils.hasText(base)) {
            return Map.of("success", false, "message", "WHATSUP_URL is not configured");
        }
        String url = base.replaceAll("/$", "") + "/logout";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(props.getApiKey())) {
                headers.set("X-API-Key", props.getApiKey());
            }
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>("{}", headers),
                    Map.class
            );
            Map body = response.getBody();
            if (body == null) {
                return Map.of("success", true, "message", "Logged out — scan new QR");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) body;
            return typed;
        } catch (Exception e) {
            log.error("WhatsApp logout failed: {}", e.getMessage());
            return Map.of(
                    "success",
                    false,
                    "message",
                    e.getMessage() == null ? "Logout failed" : e.getMessage()
            );
        }
    }

    @SuppressWarnings("rawtypes")
    private Map getJson(String url) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(props.getApiKey())) {
            headers.set("X-API-Key", props.getApiKey());
        }
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        return response.getBody();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
