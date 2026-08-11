package com.shop.mapper;

import com.shop.dto.CartItemResponse;
import com.shop.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.imageUrl", target = "productImageUrl")
    @Mapping(source = "product.price", target = "unitPrice")
    @Mapping(source = "product.stockQuantity", target = "stockQuantity")
    @Mapping(target = "lineTotal", expression = "java(lineTotal(item))")
    CartItemResponse toResponse(CartItem item);

    default BigDecimal lineTotal(CartItem item) {
        if (item.getProduct() == null || item.getProduct().getPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
