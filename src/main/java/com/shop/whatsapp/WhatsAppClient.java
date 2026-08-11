package com.shop.whatsapp;

import com.shop.config.WhatsAppProperties;
import com.shop.service.WhatsAppSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Outgoing WhatsApp only: POST {WHATSUP_URL}/send → Node microservice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppClient {

    private final WhatsAppProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public WhatsAppSendResult sendMessage(String phone, String message) {
        try {
            if (!StringUtils.hasText(phone) || !StringUtils.hasText(message)) {
                return WhatsAppSendResult.fail("Missing phone or message");
            }
            String base = props.getApiUrl();
            if (!StringUtils.hasText(base)) {
                return WhatsAppSendResult.fail("WHATSUP_URL not set");
            }

            String url = base.replaceAll("/$", "") + "/send";

            Map<String, String> body = new HashMap<>();
            body.put("phone", phone.trim());
            body.put("message", message);

            // Must be JSON — Express only parses application/json into req.body
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(props.getApiKey())) {
                headers.set("X-API-Key", props.getApiKey());
            }

            log.info("WhatsAppClient POST {} → phone={}", url, phone);
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("WhatsApp message sent to {}", phone);
            return WhatsAppSendResult.ok();
        } catch (Exception e) {
            log.error("WhatsApp sending failed: {}", e.getMessage());
            return WhatsAppSendResult.fail(e.getMessage());
        }
    }
}
