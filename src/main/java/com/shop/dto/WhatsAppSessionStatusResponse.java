package com.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppSessionStatusResponse {

    /** offline | qr_pending | connected */
    private String state;

    private boolean ready;
    private String statusMessage;
    /** PNG data URL for admin UI (from GET /qr when pending) */
    private String qrDataUrl;
    private String qrUpdatedAt;
    /** Business WhatsApp number digits when connected */
    private String linkedNumber;
    private boolean nodeReachable;
}
