package com.shop.service;

import com.shop.dto.OrderResponse;
import com.shop.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Link-mode helpers removed — WhatsApp is sent only via the Node microservice
 * from {@link OrderService} (same pattern as the defect-tracking sample).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationServiceImpl implements WhatsAppNotificationService {

    @Override
    public OrderResponse notifyAndAttachLinks(Order order, OrderResponse response) {
        // No wa.me browser links — server-side Node send only
        response.setWhatsappLink(null);
        response.setCustomerInvoiceLink(null);
        return response;
    }
}
