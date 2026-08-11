package com.shop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    /**
     * Provider: {@code meta} (WhatsApp Cloud API) or {@code node} (local whatsapp-web.js).
     */
    private String provider = "meta";

    /** Meta Graph API version, e.g. v21.0 */
    private String apiVersion = "v21.0";

    /** Permanent / system user access token from Meta Developer Console */
    private String accessToken = "";

    /**
     * WhatsApp Business Phone Number ID (sends FROM this number).
     * Found in Meta → WhatsApp → API Setup → Phone number ID.
     */
    private String phoneNumberId = "";

    /** Optional WhatsApp Business Account ID (logging / future use) */
    private String businessAccountId = "";

    /**
     * Optional approved template name for business-initiated messages.
     * Required to send from your real business number outside the 24h window.
     * Example: {@code shopease_order_invoice}
     */
    private String templateName = "";

    private String templateLanguage = "en_US";

    /**
     * If true and free-form text fails, fall back to Meta's {@code hello_world} test template
     * (sent from whatever Phone Number ID is configured — often the Meta test line).
     * Keep false when using your real business number + custom invoice template.
     */
    private boolean allowTestFallback = false;

    /** Legacy Node whatsapp-web.js base URL (used when provider=node) */
    private String apiUrl = "http://localhost:3001";

    /** Optional shared secret for Node X-API-Key */
    private String apiKey = "";

    /**
     * Store / business display number (digits, country code included), e.g. 94766292509.
     * Also used as the destination for store-order alerts.
     */
    private String storeNumber = "94766292509";

    private String defaultCountryCode = "94";

    private boolean sendCustomerInvoice = true;

    /** Also notify the store number when a new order is placed */
    private boolean sendStoreNotification = true;

    /**
     * When true and provider=node, Spring Boot starts {@code npm start} in
     * {@link #serviceDir} on ApplicationReady and stops it on shutdown.
     */
    private boolean managedNode = true;

    /**
     * Folder containing the Node WhatsApp service (package.json + index.js).
     * Absolute path, or relative to the process working directory.
     * Empty = auto-detect {@code whatsapp-service} next to / inside the repo.
     */
    private String serviceDir = "";

    /** Max seconds to wait for GET /status after spawning Node */
    private int startupTimeoutSeconds = 90;

    /** Run {@code npm install} once if node_modules is missing */
    private boolean autoNpmInstall = true;
}
