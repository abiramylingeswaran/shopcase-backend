package com.shop.controller;

import com.shop.dto.WhatsAppSessionStatusResponse;
import com.shop.service.WhatsAppSessionStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin-only WhatsApp session / QR access (shown inside the ShopEase admin UI).
 */
@RestController
@RequestMapping("/api/admin/whatsapp")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWhatsAppController {

    private final WhatsAppSessionStatusService sessionStatusService;

    @GetMapping("/status")
    public ResponseEntity<WhatsAppSessionStatusResponse> status() {
        return ResponseEntity.ok(sessionStatusService.getStatus());
    }

    /**
     * Unlink the current store WhatsApp and show a new QR so admin can scan a different number.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(sessionStatusService.logoutAndRequestNewQr());
    }
}
