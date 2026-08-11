package com.shop.service;

import com.shop.entity.Order;
import com.shop.entity.OrderItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class OrderWhatsAppMessageBuilder {

    public String buildStoreMessage(Order order) {
        String customerName = order.getUser() != null ? order.getUser().getName() : "Customer";
        return """
                *New Order #%d*
                Customer: %s
                Phone: %s
                Address: %s

                Items:
                %s

                *Total: %s*
                Status: %s
                """.formatted(
                order.getId(),
                customerName,
                order.getPhone(),
                order.getDeliveryAddress(),
                formatItems(order),
                formatMoney(order.getTotalAmount()),
                order.getStatus()
        ).trim();
    }

    /** Invoice text sent / opened for the customer. */
    public String buildCustomerInvoice(Order order) {
        String customerName = order.getUser() != null ? order.getUser().getName() : "Customer";
        return """
                *ShopEase — Order Invoice*
                Order #%d
                Hi %s,

                Thanks for your order. Here is your invoice:

                Items:
                %s

                *Total: %s*
                Status: %s

                Delivery:
                %s
                Phone: %s

                Thank you for shopping with ShopEase!
                """.formatted(
                order.getId(),
                customerName,
                formatItems(order),
                formatMoney(order.getTotalAmount()),
                order.getStatus(),
                order.getDeliveryAddress(),
                order.getPhone()
        ).trim();
    }

    /**
     * Body variables for Meta template {@code shopease_order_invoice}:
     * {{1}} name, {{2}} order id, {{3}} total, {{4}} status, {{5}} delivery summary.
     */
    public java.util.List<String> buildInvoiceTemplateParams(Order order) {
        String customerName = order.getUser() != null ? order.getUser().getName() : "Customer";
        String delivery = "%s | Phone: %s".formatted(
                order.getDeliveryAddress() == null ? "-" : order.getDeliveryAddress(),
                order.getPhone() == null ? "-" : order.getPhone()
        );
        String items = formatItemsOneLine(order);
        if (StringUtils.hasText(items)) {
            delivery = delivery + " | Items: " + items;
        }
        return java.util.List.of(
                sanitizeTemplateParam(customerName),
                sanitizeTemplateParam(String.valueOf(order.getId())),
                sanitizeTemplateParam(formatMoney(order.getTotalAmount())),
                sanitizeTemplateParam(order.getStatus() == null ? "PENDING" : order.getStatus().name()),
                sanitizeTemplateParam(delivery)
        );
    }

    /** Store-alert template params (same shape as invoice for reuse). */
    public java.util.List<String> buildStoreTemplateParams(Order order) {
        return buildInvoiceTemplateParams(order);
    }

    public String sanitizeTemplateParam(String raw) {
        if (raw == null) {
            return "-";
        }
        // Meta rejects newlines / tabs and long runs of spaces in template variables
        String cleaned = raw.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
                .replaceAll(" {4,}", "   ")
                .trim();
        if (cleaned.isEmpty()) {
            return "-";
        }
        return cleaned.length() > 900 ? cleaned.substring(0, 900) + "…" : cleaned;
    }

    public String buildClickToChatLink(String phoneDigits, String message) {
        String digits = phoneDigits == null ? "" : phoneDigits.replaceAll("\\D", "");
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + digits + "?text=" + encoded;
    }

    /**
     * Normalize local phones like 0766292509 to international digits (94766292509).
     */
    public String normalizePhoneDigits(String rawPhone, String defaultCountryCode) {
        if (!StringUtils.hasText(rawPhone)) {
            return "";
        }
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.startsWith("0") && StringUtils.hasText(defaultCountryCode)) {
            digits = defaultCountryCode.replaceAll("\\D", "") + digits.substring(1);
        }
        return digits;
    }

    private String formatItems(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "(no items)";
        }
        return order.getItems().stream()
                .map(this::formatItem)
                .collect(Collectors.joining("\n"));
    }

    private String formatItemsOneLine(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "";
        }
        return order.getItems().stream()
                .map(item -> {
                    String name = item.getProduct() != null ? item.getProduct().getName() : "Product";
                    return name + " x" + item.getQuantity();
                })
                .collect(Collectors.joining(", "));
    }

    private String formatItem(OrderItem item) {
        String name = item.getProduct() != null ? item.getProduct().getName() : "Product";
        BigDecimal line = item.getPriceAtPurchase()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return "- %s x%d @ %s = %s".formatted(
                name,
                item.getQuantity(),
                formatMoney(item.getPriceAtPurchase()),
                formatMoney(line)
        );
    }

    private String formatMoney(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }
}
