package com.shop.service;

import com.shop.entity.Order;
import com.shop.entity.User;
import com.shop.repository.UserRepository;
import com.shop.whatsapp.WhatsAppClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Async one-way WhatsApp notifications via Node POST /send.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationDispatcher {

    private final WhatsAppClient whatsAppClient;
    private final WhatsAppTemplateService templateService;
    private final UserRepository userRepository;

    @Async
    public void sendEvent(Long userId, String eventType, Map<String, String> variables, String phoneOverride) {
        try {
            User user = userId == null ? null : userRepository.findById(userId).orElse(null);
            String phone = StringUtils.hasText(phoneOverride)
                    ? phoneOverride
                    : (user != null ? user.getPhone() : null);
            if (!StringUtils.hasText(phone)) {
                log.warn("WhatsApp skip {} — no phone for userId={}", eventType, userId);
                return;
            }
            Map<String, String> vars = variables == null ? Map.of() : variables;
            String message = templateService.buildMessage(eventType, vars, user);
            whatsAppClient.sendMessage(phone, message);
        } catch (Exception e) {
            log.error("WhatsApp async send failed event={} userId={}: {}", eventType, userId, e.getMessage());
        }
    }

    @Async
    public void sendOrderPlaced(Order order) {
        if (order == null || order.getUser() == null) {
            return;
        }
        String phone = StringUtils.hasText(order.getPhone()) ? order.getPhone() : order.getUser().getPhone();
        sendEvent(order.getUser().getId(), "ORDER_PLACED", templateService.orderVariables(order), phone);
    }

    @Async
    public void sendOrderStatusUpdated(Order order, String oldStatus, String newStatus) {
        if (order == null || order.getUser() == null) {
            return;
        }
        Map<String, String> vars = new HashMap<>(templateService.orderVariables(order));
        vars.put("oldStatus", oldStatus == null ? "-" : oldStatus);
        vars.put("newStatus", newStatus == null ? "-" : newStatus);
        String phone = StringUtils.hasText(order.getPhone()) ? order.getPhone() : order.getUser().getPhone();
        sendEvent(order.getUser().getId(), "ORDER_STATUS_UPDATED", vars, phone);
    }
}
