package com.shop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "ShopEase");
        body.put("tagline", "Electronics, fashion & home — order in minutes");
        body.put("description",
                "A role-based e-commerce app. Guests can read about ShopEase; "
                        + "customers browse products and order; admins manage catalog and orders.");
        body.put("features", List.of(
                "JWT authentication with ADMIN and CUSTOMER roles",
                "Protected product catalog (login required)",
                "Cart and checkout with order history",
                "Admin dashboard for products and orders",
                "Server-side role enforcement with Spring Security"
        ));
        body.put("roles", List.of(
                Map.of("role", "GUEST", "access", "About page, login, and register only"),
                Map.of("role", "CUSTOMER", "access", "Products, cart, checkout, own orders"),
                Map.of("role", "ADMIN", "access", "Everything customers see, plus admin dashboard")
        ));
        return ResponseEntity.ok(body);
    }
}
