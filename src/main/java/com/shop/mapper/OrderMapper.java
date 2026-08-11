package com.shop.mapper;

import com.shop.dto.OrderItemResponse;
import com.shop.dto.OrderResponse;
import com.shop.entity.Order;
import com.shop.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "customerName")
    @Mapping(source = "user.email", target = "customerEmail")
    @Mapping(target = "whatsappLink", ignore = true)
    @Mapping(target = "customerInvoiceLink", ignore = true)
    @Mapping(target = "customerInvoiceSent", ignore = true)
    @Mapping(target = "whatsappSent", ignore = true)
    @Mapping(target = "whatsappError", ignore = true)
    @Mapping(target = "whatsappInfo", ignore = true)
    OrderResponse toResponse(Order order);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(target = "lineTotal", expression = "java(lineTotal(item))")
    OrderItemResponse toItemResponse(OrderItem item);

    default BigDecimal lineTotal(OrderItem item) {
        if (item.getPriceAtPurchase() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
