package com.shop.service;

import com.shop.config.WhatsAppProperties;
import com.shop.dto.CheckoutRequest;
import com.shop.dto.OrderResponse;
import com.shop.dto.UpdateOrderStatusRequest;
import com.shop.entity.*;
import com.shop.exception.BadRequestException;
import com.shop.exception.ResourceNotFoundException;
import com.shop.mapper.OrderMapper;
import com.shop.repository.OrderRepository;
import com.shop.repository.ProductRepository;
import com.shop.repository.UserRepository;
import com.shop.security.SecurityUtils;
import com.shop.whatsapp.WhatsAppClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final WhatsAppNotificationService whatsAppNotificationService;
    private final WhatsAppProperties whatsAppProperties;
    private final WhatsAppTemplateService whatsAppTemplateService;
    private final WhatsAppClient whatsAppClient;

    /**
     * Checkout flow:
     * 1) Save order (checkout form phone)
     * 2) Fire-and-forget WhatsApp via Node microservice POST /send
     * 3) Never fail the order if WhatsApp fails
     */
    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Long userId = SecurityUtils.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String customerPhone = request.getPhone() == null ? "" : request.getPhone().trim();
        if (!StringUtils.hasText(customerPhone)) {
            throw new BadRequestException("Customer phone is required at checkout");
        }

        log.info(
                "Checkout phone source=CheckoutRequest phone={} (account phone ignored={})",
                customerPhone,
                user.getPhone()
        );

        List<CartItem> cartItems = cartService.getCartEntitiesForCheckout(userId);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .deliveryAddress(request.getDeliveryAddress().trim())
                .phone(customerPhone)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            if (product.getStockQuantity() < quantity) {
                throw new BadRequestException(
                        "Insufficient stock for product: " + product.getName()
                );
            }

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);

            BigDecimal unitPrice = product.getPrice();
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .priceAtPurchase(unitPrice)
                    .build();
            order.addItem(orderItem);
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        cartService.clearCart(userId);

        Order complete = orderRepository.findByIdWithItems(saved.getId())
                .orElse(saved);

        String destinationPhone = complete.getPhone();
        boolean whatsappSent = false;
        String whatsappError = null;
        String whatsappInfo = null;

        try {
            if (whatsAppProperties.isSendCustomerInvoice()) {
                String message = whatsAppTemplateService.buildMessage(
                        "ORDER_PLACED",
                        whatsAppTemplateService.orderVariables(complete),
                        complete.getUser()
                );
                // Node microservice only: POST /send { phone, message }
                WhatsAppSendResult result = whatsAppClient.sendMessage(destinationPhone, message);
                whatsappSent = result.sent();
                whatsappError = result.error();
                if (whatsappSent) {
                    whatsappInfo = "Order confirmation sent via WhatsApp.";
                    log.info("WhatsApp SENT → phone={} orderId={}", destinationPhone, complete.getId());
                } else {
                    log.warn("WhatsApp NOT sent → phone={} orderId={} error={}",
                            destinationPhone, complete.getId(), whatsappError);
                }
            }
        } catch (Exception ex) {
            whatsappError = ex.getMessage();
            log.error("WhatsApp send FAILED → phone={} orderId={}: {}",
                    destinationPhone, complete.getId(), ex.getMessage());
        }

        OrderResponse response = orderMapper.toResponse(complete);
        response.setWhatsappSent(whatsappSent);
        response.setCustomerInvoiceSent(whatsappSent);
        response.setWhatsappError(whatsappError);
        response.setWhatsappInfo(whatsappInfo);
        return whatsAppNotificationService.notifyAndAttachLinks(complete, response);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        Long userId = SecurityUtils.currentUserId();
        return orderRepository.findByUserIdWithItems(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithItems().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        String oldStatus = order.getStatus() == null ? null : order.getStatus().name();
        order.setStatus(request.getStatus());
        Order saved = orderRepository.save(order);
        Order complete = orderRepository.findByIdWithItems(saved.getId()).orElse(saved);
        try {
            String phone = StringUtils.hasText(complete.getPhone())
                    ? complete.getPhone()
                    : (complete.getUser() != null ? complete.getUser().getPhone() : null);
            if (StringUtils.hasText(phone)) {
                java.util.Map<String, String> vars = new java.util.HashMap<>(
                        whatsAppTemplateService.orderVariables(complete)
                );
                vars.put("oldStatus", oldStatus == null ? "-" : oldStatus);
                vars.put("newStatus", request.getStatus() == null ? "-" : request.getStatus().name());
                String message = whatsAppTemplateService.buildMessage(
                        "ORDER_STATUS_UPDATED",
                        vars,
                        complete.getUser()
                );
                whatsAppClient.sendMessage(phone, message);
            }
        } catch (Exception e) {
            log.warn("WhatsApp status notify failed orderId={}: {}", orderId, e.getMessage());
        }
        return orderMapper.toResponse(complete);
    }
}
