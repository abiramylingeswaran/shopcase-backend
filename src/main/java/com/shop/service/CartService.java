package com.shop.service;

import com.shop.dto.AddToCartRequest;
import com.shop.dto.CartItemResponse;
import com.shop.dto.CartResponse;
import com.shop.dto.UpdateCartItemRequest;
import com.shop.entity.CartItem;
import com.shop.entity.Product;
import com.shop.entity.User;
import com.shop.exception.BadRequestException;
import com.shop.exception.ResourceNotFoundException;
import com.shop.mapper.CartMapper;
import com.shop.repository.CartItemRepository;
import com.shop.repository.UserRepository;
import com.shop.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final CartMapper cartMapper;

    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Long userId = SecurityUtils.currentUserId();
        List<CartItem> items = cartItemRepository.findByUserIdWithProduct(userId);
        return toCartResponse(items);
    }

    @Transactional
    public CartResponse addItem(AddToCartRequest request) {
        Long userId = SecurityUtils.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productService.getEntity(request.getProductId());

        validateStock(product, request.getQuantity());

        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, product.getId())
                .map(existing -> {
                    int newQty = existing.getQuantity() + request.getQuantity();
                    validateStock(product, newQty);
                    existing.setQuantity(newQty);
                    return existing;
                })
                .orElseGet(() -> CartItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(request.getQuantity())
                        .build());

        cartItemRepository.save(item);
        return getCart();
    }

    @Transactional
    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
        Long userId = SecurityUtils.currentUserId();
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        validateStock(item.getProduct(), request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return getCart();
    }

    @Transactional
    public CartResponse removeItem(Long itemId) {
        Long userId = SecurityUtils.currentUserId();
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
        cartItemRepository.delete(item);
        return getCart();
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartEntitiesForCheckout(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserIdWithProduct(userId);
        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        return items;
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private void validateStock(Product product, int quantity) {
        if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
            throw new BadRequestException(
                    "Insufficient stock for product: " + product.getName()
                            + " (available: " + product.getStockQuantity() + ")"
            );
        }
    }

    private CartResponse toCartResponse(List<CartItem> items) {
        List<CartItemResponse> responses = items.stream()
                .map(cartMapper::toResponse)
                .toList();

        BigDecimal total = responses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = responses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .items(responses)
                .totalAmount(total)
                .itemCount(itemCount)
                .build();
    }
}
