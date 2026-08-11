package com.shop.dto;

import lombok.Data;

@Data
public class WhatsappBotRequest {
    private String whatsappId;
    private String code;
    private Long orderId;
}
