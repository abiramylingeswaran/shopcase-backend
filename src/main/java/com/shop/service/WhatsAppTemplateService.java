package com.shop.service;

import com.shop.entity.Order;
import com.shop.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Defect Tracker–style event templates (emoji + plain WhatsApp markdown).
 */
@Service
public class WhatsAppTemplateService {

    public String buildMessage(String eventType, Map<String, String> variables, User user) {
        String name = user != null && StringUtils.hasText(user.getName()) ? user.getName() : "Customer";
        return switch (eventType == null ? "" : eventType.toUpperCase()) {
            case "USER_REGISTERED" -> userRegistered(name, variables);
            case "WHATSAPP_VERIFY" -> verifyPrompt(variables);
            case "ORDER_PLACED" -> orderPlaced(name, variables);
            case "ORDER_STATUS_UPDATED" -> orderStatusUpdated(name, variables);
            case "PASSWORD_CHANGED" -> passwordChanged(name);
            default -> "ShopEase notification: " + eventType;
        } + footer();
    }

    public String buildVerifyPrompt(String code) {
        return """
                🔐 *WhatsApp Verification Required*

                Your verification code is:

                👉 *%s*

                Please reply with:
                ✅ *VERIFY %s*
                """.formatted(code, code).trim() + footer();
    }

    public Map<String, String> orderVariables(Order order) {
        String items = order.getItems() == null ? "-" : order.getItems().stream()
                .map(i -> {
                    String n = i.getProduct() != null ? i.getProduct().getName() : "Item";
                    return n + " x" + i.getQuantity();
                })
                .collect(Collectors.joining(", "));
        return Map.of(
                "orderId", String.valueOf(order.getId()),
                "total", order.getTotalAmount() == null ? "0.00" : order.getTotalAmount().toPlainString(),
                "status", order.getStatus() == null ? "PENDING" : order.getStatus().name(),
                "address", order.getDeliveryAddress() == null ? "-" : order.getDeliveryAddress(),
                "phone", order.getPhone() == null ? "-" : order.getPhone(),
                "items", items
        );
    }

    private String userRegistered(String name, Map<String, String> v) {
        return """
                👋 *Welcome to ShopEase*

                Hello %s,

                Your account was created successfully.
                Email: %s

                Reply *HELP* anytime for WhatsApp commands.
                """.formatted(name, v.getOrDefault("email", "-")).trim();
    }

    private String verifyPrompt(Map<String, String> v) {
        String code = v.getOrDefault("code", "------");
        return """
                🔐 *WhatsApp Verification Required*

                Your verification code is:

                👉 *%s*

                Please reply with:
                ✅ *VERIFY %s*
                """.formatted(code, code).trim();
    }

    private String orderPlaced(String name, Map<String, String> v) {
        return """
                🧾 *ORDER PLACED*

                Hello %s,

                Thanks for your ShopEase order.

                Order ID: #%s
                Items: %s
                Total: %s
                Status: %s
                Delivery: %s
                Phone: %s

                Reply *ORDERS* to list your recent orders.
                """.formatted(
                name,
                v.getOrDefault("orderId", "-"),
                v.getOrDefault("items", "-"),
                v.getOrDefault("total", "-"),
                v.getOrDefault("status", "-"),
                v.getOrDefault("address", "-"),
                v.getOrDefault("phone", "-")
        ).trim();
    }

    private String orderStatusUpdated(String name, Map<String, String> v) {
        return """
                📦 *ORDER STATUS UPDATED*

                Hello %s,

                Order #%s status changed.

                Previous: %s
                New: %s

                Reply *ORDER %s* for details.
                """.formatted(
                name,
                v.getOrDefault("orderId", "-"),
                v.getOrDefault("oldStatus", "-"),
                v.getOrDefault("newStatus", "-"),
                v.getOrDefault("orderId", "-")
        ).trim();
    }

    private String passwordChanged(String name) {
        return """
                🔒 *PASSWORD CHANGED*

                Hello %s,

                Your ShopEase password was changed successfully.
                If this wasn't you, contact support immediately.
                """.formatted(name).trim();
    }

    private String footer() {
        return "\n\n© " + Year.now().getValue() + " ShopEase";
    }
}
