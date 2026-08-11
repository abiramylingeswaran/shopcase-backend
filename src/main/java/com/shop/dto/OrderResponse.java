package com.shop.dto;

import com.shop.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String deliveryAddress;
    private String phone;
    private Instant createdAt;
    private List<OrderItemResponse> items;

    /**
     * True when the invoice WhatsApp was sent server-side to the customer phone.
     * Never implies browser navigation to WhatsApp.
     */
    private boolean whatsappSent;

    /** Meta / provider error when {@link #whatsappSent} is false */
    private String whatsappError;

    /** Extra note when send was accepted (e.g. hello_world fallback, test sender) */
    private String whatsappInfo;

    /** @deprecated use {@link #whatsappSent} */
    private boolean customerInvoiceSent;

    /** @deprecated link-mode only; not used for Twilio auto-send */
    private String whatsappLink;

    /** @deprecated link-mode only; not used for Twilio auto-send */
    private String customerInvoiceLink;
}
