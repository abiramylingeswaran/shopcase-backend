package com.shop.service;

import com.shop.dto.OrderResponse;
import com.shop.entity.Order;

/**
 * Sends / prepares WhatsApp notifications after checkout:
 * - store owner alert
 * - customer invoice to the phone entered at checkout
 */
public interface WhatsAppNotificationService {

    /**
     * Notify store + prepare/send customer invoice.
     * Sets {@code whatsappLink} (store) and {@code customerInvoiceLink} on the response.
     */
    OrderResponse notifyAndAttachLinks(Order order, OrderResponse response);
}
