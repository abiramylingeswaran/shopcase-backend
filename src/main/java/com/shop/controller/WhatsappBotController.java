package com.shop.controller;

import com.shop.dto.WhatsappBotRequest;
import com.shop.entity.Order;
import com.shop.entity.User;
import com.shop.repository.OrderRepository;
import com.shop.repository.UserRepository;
import com.shop.service.WhatsAppVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public bot APIs called by the Node WhatsApp listener (Defect Tracker pattern).
 */
@Slf4j
@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsappBotController {

    private final WhatsAppVerificationService verificationService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = verificationService.verify(
                    request.get("code"),
                    request.get("whatsappId")
            );
            response.put("success", result.startsWith("✅"));
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Verification error", e);
            response.put("success", false);
            response.put("message", "Verification failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/my-orders")
    public ResponseEntity<?> myOrders(@RequestBody WhatsappBotRequest request) {
        try {
            User user = requireVerifiedUser(request.getWhatsappId());
            if (user == null) {
                return notVerified();
            }
            List<Order> orders = orderRepository.findByUserIdWithItems(user.getId());
            if (orders.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(orders.stream().limit(10).map(this::orderSummary).toList());
        } catch (Exception e) {
            log.error("Error fetching orders", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error fetching orders: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/order")
    public ResponseEntity<?> orderDetail(@RequestBody WhatsappBotRequest request) {
        try {
            User user = requireVerifiedUser(request.getWhatsappId());
            if (user == null) {
                return notVerified();
            }
            if (request.getOrderId() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Usage: ORDER <id>  e.g. ORDER 12"
                ));
            }
            Order order = orderRepository.findByIdWithItems(request.getOrderId()).orElse(null);
            if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Order not found for your account."
                ));
            }
            return ResponseEntity.ok(orderSummary(order));
        } catch (Exception e) {
            log.error("Error fetching order", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error fetching order: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/help")
    public ResponseEntity<Map<String, Object>> help(@RequestBody WhatsappBotRequest request) {
        boolean verified = requireVerifiedUser(request.getWhatsappId()) != null;
        String message = """
                🛒 *ShopEase WhatsApp Bot*

                *VERIFY <code>* — link your account
                *ORDERS* — your recent orders
                *ORDER <id>* — order details
                *HELP* — this message

                %s
                """.formatted(verified
                ? "✅ Your WhatsApp is verified."
                : "⚠️ Not verified yet. Use VERIFY <code> from your registration SMS/WhatsApp."
        ).trim();
        return ResponseEntity.ok(Map.of("success", true, "message", message));
    }

    private User requireVerifiedUser(String whatsappId) {
        if (!StringUtils.hasText(whatsappId)) {
            return null;
        }
        return userRepository.findByWhatsappId(whatsappId.trim()).orElse(null);
    }

    private ResponseEntity<Map<String, Object>> notVerified() {
        return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "WhatsApp not verified. Please use VERIFY <code> first."
        ));
    }

    private Map<String, Object> orderSummary(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getId());
        map.put("status", order.getStatus() == null ? "N/A" : order.getStatus().name());
        map.put("total", order.getTotalAmount() == null ? "0.00" : order.getTotalAmount().toPlainString());
        map.put("address", order.getDeliveryAddress());
        map.put("phone", order.getPhone());
        map.put("createdAt", order.getCreatedAt() == null ? null : order.getCreatedAt().toString());
        if (order.getItems() != null) {
            map.put("items", order.getItems().stream().map(i -> {
                String name = i.getProduct() != null ? i.getProduct().getName() : "Item";
                return name + " x" + i.getQuantity();
            }).toList());
        }
        return map;
    }
}
