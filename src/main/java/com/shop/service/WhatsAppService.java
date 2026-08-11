package com.shop.service;

import com.shop.config.WhatsAppProperties;
import com.shop.whatsapp.WhatsAppClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends WhatsApp messages via Meta Cloud API or the managed Node whatsapp-web.js service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final WhatsAppProperties props;
    private final OrderWhatsAppMessageBuilder messageBuilder;
    private final WhatsAppClient whatsAppClient;

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(8))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();

    @PostConstruct
    void init() {
        String provider = normalizeProvider();
        if ("meta".equals(provider)) {
            boolean ready = StringUtils.hasText(props.getAccessToken())
                    && StringUtils.hasText(props.getPhoneNumberId());
            log.info(
                    "WhatsAppService: provider=meta phoneNumberId={} ready={} tokenLen={} storeNumber={} template={}",
                    props.getPhoneNumberId(),
                    ready,
                    props.getAccessToken() == null ? 0 : props.getAccessToken().trim().length(),
                    props.getStoreNumber(),
                    props.getTemplateName()
            );
            if (!ready) {
                log.warn(
                        "Meta WhatsApp is not fully configured. Set WHATSAPP_ACCESS_TOKEN and WHATSAPP_PHONE_NUMBER_ID."
                );
            }
        } else {
            log.info("WhatsAppService: provider=node apiUrl={}", props.getApiUrl());
        }
    }

    public boolean canAutoSend() {
        if ("meta".equals(normalizeProvider())) {
            return StringUtils.hasText(props.getAccessToken())
                    && StringUtils.hasText(props.getPhoneNumberId());
        }
        return StringUtils.hasText(props.getApiUrl());
    }

    /** Checkout helper — send FROM Meta business number TO customer phone. */
    public WhatsAppSendResult sendFromBusiness(String toPhoneRaw, String body) {
        return sendMessage(toPhoneRaw, body);
    }

    /**
     * Send order invoice FROM your business Phone Number ID.
     * Uses approved {@code WHATSAPP_TEMPLATE_NAME} when set (required for cold outbound).
     */
    public WhatsAppSendResult sendCustomerInvoice(String toPhoneRaw, com.shop.entity.Order order) {
        if (order == null) {
            return WhatsAppSendResult.fail("Missing order");
        }
        String digits = messageBuilder.normalizePhoneDigits(toPhoneRaw, props.getDefaultCountryCode());
        if (!StringUtils.hasText(digits)) {
            return WhatsAppSendResult.fail("Invalid phone: " + toPhoneRaw);
        }
        if ("meta".equals(normalizeProvider()) && StringUtils.hasText(props.getTemplateName())) {
            return sendViaMetaTemplate(
                    digits,
                    props.getTemplateName().trim(),
                    messageBuilder.buildInvoiceTemplateParams(order)
            );
        }
        return sendMessage(toPhoneRaw, messageBuilder.buildCustomerInvoice(order));
    }

    /** Store new-order alert (same custom template when configured). */
    public WhatsAppSendResult sendStoreNotification(String toPhoneRaw, com.shop.entity.Order order) {
        if (order == null) {
            return WhatsAppSendResult.fail("Missing order");
        }
        String digits = messageBuilder.normalizePhoneDigits(toPhoneRaw, props.getDefaultCountryCode());
        if (!StringUtils.hasText(digits)) {
            return WhatsAppSendResult.fail("Invalid phone: " + toPhoneRaw);
        }
        if ("meta".equals(normalizeProvider()) && StringUtils.hasText(props.getTemplateName())) {
            return sendViaMetaTemplate(
                    digits,
                    props.getTemplateName().trim(),
                    messageBuilder.buildStoreTemplateParams(order)
            );
        }
        return sendMessage(toPhoneRaw, messageBuilder.buildStoreMessage(order));
    }

    /**
     * Same idea as Defect Tracker {@code WhatsAppService.sendMessage}:
     * Node provider → POST {WHATSUP_URL}/send with the phone string as stored (e.g. 0766292509).
     * Meta provider still needs normalized E.164 digits.
     */
    public WhatsAppSendResult sendMessage(String phone, String message) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(message)) {
            return WhatsAppSendResult.fail("Missing phone or message");
        }

        if ("meta".equals(normalizeProvider())) {
            String digits = messageBuilder.normalizePhoneDigits(phone, props.getDefaultCountryCode());
            if (!StringUtils.hasText(digits)) {
                return WhatsAppSendResult.fail("Invalid phone: " + phone);
            }
            return sendViaMeta(digits, message);
        }

        // Defect Tracker: pass phone through unchanged (contactNo / checkout phone / whatsappId)
        return sendViaNode(phone.trim(), message);
    }

    public String businessNumberDigits() {
        return messageBuilder.normalizePhoneDigits(
                props.getStoreNumber(),
                props.getDefaultCountryCode()
        );
    }

    private WhatsAppSendResult sendViaMeta(String toDigits, String message) {
        if (!canAutoSend()) {
            return WhatsAppSendResult.fail(
                    "Meta credentials missing. Set WHATSAPP_ACCESS_TOKEN and WHATSAPP_PHONE_NUMBER_ID, then restart backend."
            );
        }

        // Prefer free-form invoice text when possible (works inside Meta's 24h window).
        if (!StringUtils.hasText(props.getTemplateName())) {
            WhatsAppSendResult textResult = postMeta(toDigits, buildTextPayload(toDigits, message));
            if (textResult.sent()) {
                return textResult;
            }

            // Optional: Meta test template (usually from +1 555 test line — not for production invoices)
            if (props.isAllowTestFallback() && isOutsideCustomerCareWindow(textResult.error())) {
                log.warn(
                        "WhatsApp free-form failed ({}). Retrying with Meta test template hello_world → to={}",
                        textResult.error(),
                        toDigits
                );
                WhatsAppSendResult templateResult = postMeta(
                        toDigits,
                        buildTemplatePayload(toDigits, "hello_world", props.getTemplateLanguage(), List.of())
                );
                if (templateResult.sent()) {
                    return WhatsAppSendResult.ok(
                            "Meta accepted hello_world from the configured Phone Number ID (not a custom invoice). "
                                    + "Create an approved template and set WHATSAPP_TEMPLATE_NAME to send real invoices."
                    );
                }
                return WhatsAppSendResult.fail(explainMetaError(
                        templateResult.error() != null ? templateResult.error() : textResult.error()
                ));
            }
            if (isOutsideCustomerCareWindow(textResult.error())) {
                return WhatsAppSendResult.fail(
                        "Meta blocks free-form text outside the 24h window. "
                                + "Create an approved invoice template in Meta, set WHATSAPP_TEMPLATE_NAME, "
                                + "and use your business Phone Number ID. Details: " + textResult.error()
                );
            }
            return WhatsAppSendResult.fail(explainMetaError(textResult.error()));
        }

        // Configured custom template (business-initiated)
        return sendViaMetaTemplate(
                toDigits,
                props.getTemplateName().trim(),
                List.of(messageBuilder.sanitizeTemplateParam(message))
        );
    }

    private WhatsAppSendResult sendViaMetaTemplate(
            String toDigits,
            String templateName,
            List<String> bodyParams
    ) {
        WhatsAppSendResult result = postMeta(
                toDigits,
                buildTemplatePayload(toDigits, templateName, props.getTemplateLanguage(), bodyParams)
        );
        if (result.sent()) {
            return WhatsAppSendResult.ok(
                    "Invoice template \"" + templateName + "\" accepted by Meta for " + toDigits
                            + ". It is sent FROM your configured business Phone Number ID."
            );
        }
        return WhatsAppSendResult.fail(explainMetaError(result.error()));
    }

    private WhatsAppSendResult postMeta(String toDigits, Map<String, Object> payload) {
        String url = String.format(
                "https://graph.facebook.com/%s/%s/messages",
                props.getApiVersion().replaceAll("^/+", "").replaceAll("/+$", ""),
                props.getPhoneNumberId().trim()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getAccessToken().trim());

            log.info(
                    "WhatsApp Meta ABOUT TO SEND → fromPhoneNumberId={} to={} type={} url={}",
                    props.getPhoneNumberId(),
                    toDigits,
                    payload.get("type"),
                    url
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp Meta message sent → to={} body={}", toDigits, response.getBody());
                return WhatsAppSendResult.ok();
            }
            String err = "HTTP " + response.getStatusCode() + ": " + response.getBody();
            log.error("WhatsApp Meta send failed {}", err);
            return WhatsAppSendResult.fail(err);
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.error("WhatsApp Meta send HTTP error status={} body={}", e.getStatusCode(), body);
            return WhatsAppSendResult.fail("HTTP " + e.getStatusCode().value() + ": " + body);
        } catch (Exception e) {
            log.error("WhatsApp Meta send failed: {}", e.getMessage());
            return WhatsAppSendResult.fail(e.getMessage());
        }
    }

    private Map<String, Object> buildTextPayload(String toDigits, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", toDigits);
        payload.put("type", "text");
        Map<String, Object> text = new HashMap<>();
        text.put("preview_url", false);
        text.put("body", message);
        payload.put("text", text);
        return payload;
    }

    private Map<String, Object> buildTemplatePayload(
            String toDigits,
            String templateName,
            String language,
            List<String> bodyVariables
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", toDigits);
        payload.put("type", "template");

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", StringUtils.hasText(language) ? language : "en_US"));

        if (bodyVariables != null && !bodyVariables.isEmpty()
                && !"hello_world".equalsIgnoreCase(templateName.trim())) {
            List<Map<String, String>> parameters = bodyVariables.stream()
                    .map(v -> Map.of("type", "text", "text", v == null || v.isBlank() ? "-" : v))
                    .toList();
            Map<String, Object> bodyComponent = new LinkedHashMap<>();
            bodyComponent.put("type", "body");
            bodyComponent.put("parameters", parameters);
            template.put("components", List.of(bodyComponent));
        }

        payload.put("template", template);
        return payload;
    }

    private boolean isOutsideCustomerCareWindow(String error) {
        if (error == null) {
            return false;
        }
        String e = error.toLowerCase();
        return e.contains("131047")
                || e.contains("re-engagement")
                || e.contains("24 hour")
                || e.contains("24-hour")
                || e.contains("outside");
    }

    private boolean isRecipientNotAllowed(String error) {
        if (error == null) {
            return false;
        }
        String e = error.toLowerCase();
        return e.contains("131030")
                || e.contains("not in allowed list")
                || e.contains("recipient phone number not in");
    }

    private String explainMetaError(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "Meta WhatsApp send failed";
        }
        if (isRecipientNotAllowed(raw)) {
            return "Meta rejected the recipient. In Meta Developer Console → WhatsApp → API Setup, "
                    + "add this phone under the test recipient list (To). Details: " + raw;
        }
        if (isOutsideCustomerCareWindow(raw)) {
            return "Meta blocks free-form text outside the 24h window. "
                    + "Customer must message your business first, or set an approved WHATSAPP_TEMPLATE_NAME. "
                    + "Details: " + raw;
        }
        if (raw.contains("190") || raw.toLowerCase().contains("access token")
                || raw.toLowerCase().contains("session has expired")) {
            return "Meta access token is invalid or expired. Generate a new token and update "
                    + "WHATSAPP_ACCESS_TOKEN in backend/.env, then restart. Details: " + raw;
        }
        if (raw.contains("133010") || raw.toLowerCase().contains("account not registered")) {
            return "Your business WhatsApp number is not registered for Cloud API yet. "
                    + "In Meta WhatsApp Manager, finish Cloud API setup / register the number "
                    + "(not the +1 555 test line). Then use that number's Phone Number ID. Details: " + raw;
        }
        if (raw.contains("\"code\":100") || raw.contains("error_subcode\":33")
                || (raw.toLowerCase().contains("does not exist") && raw.toLowerCase().contains("permissions"))) {
            return "Wrong ID or missing access. Use Phone number ID (not WhatsApp Business Account ID) "
                    + "from Meta → WhatsApp → API Setup for your business number. Details: " + raw;
        }
        if (raw.toLowerCase().contains("template")) {
            return "Template problem — create/approve the template in Meta and set WHATSAPP_TEMPLATE_NAME "
                    + "to the exact name + matching WHATSAPP_TEMPLATE_LANGUAGE. Details: " + raw;
        }
        if (raw.toLowerCase().contains("100") && raw.toLowerCase().contains("phone")) {
            return "Check WHATSAPP_PHONE_NUMBER_ID. Details: " + raw;
        }
        return raw;
    }

    private WhatsAppSendResult sendViaNode(String phoneOrChatId, String message) {
        return whatsAppClient.sendMessage(phoneOrChatId, message);
    }

    private String normalizeProvider() {
        String p = props.getProvider();
        if (!StringUtils.hasText(p)) {
            return "meta";
        }
        return p.trim().toLowerCase();
    }
}
