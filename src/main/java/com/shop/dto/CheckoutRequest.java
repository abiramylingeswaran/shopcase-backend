package com.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank
    @Size(min = 5, max = 500)
    private String deliveryAddress;

    @NotBlank
    @Size(min = 7, max = 20)
    private String phone;
}
