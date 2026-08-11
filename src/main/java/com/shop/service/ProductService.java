package com.shop.service;

import com.shop.dto.PageResponse;
import com.shop.dto.ProductRequest;
import com.shop.dto.ProductResponse;
import com.shop.entity.Category;
import com.shop.entity.Product;
import com.shop.exception.BadRequestException;
import com.shop.exception.ResourceNotFoundException;
import com.shop.mapper.ProductMapper;
import com.shop.repository.CartItemRepository;
import com.shop.repository.OrderItemRepository;
import com.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(Long categoryId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<ProductResponse> result = productRepository
                .findFiltered(categoryId, normalizedSearch, pageable)
                .map(productMapper::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request, MultipartFile image) {
        Category category = categoryService.getEntity(request.getCategoryId());
        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        if (image != null && !image.isEmpty()) {
            product.setImageUrl(fileStorageService.store(image));
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, MultipartFile image) {
        Product product = getEntity(id);
        productMapper.updateEntity(request, product);
        product.setCategory(categoryService.getEntity(request.getCategoryId()));

        if (image != null && !image.isEmpty()) {
            String oldUrl = product.getImageUrl();
            product.setImageUrl(fileStorageService.store(image));
            fileStorageService.deleteByUrl(oldUrl);
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = getEntity(id);

        if (orderItemRepository.existsByProductId(id)) {
            throw new BadRequestException(
                    "Cannot delete \"" + product.getName()
                            + "\" because it appears in existing customer orders. "
                            + "Set stock to 0 to mark it unavailable instead."
            );
        }

        // Safe to clear active carts that still reference this product
        cartItemRepository.deleteByProductId(id);

        String imageUrl = product.getImageUrl();
        productRepository.delete(product);
        fileStorageService.deleteByUrl(imageUrl);
    }

    @Transactional(readOnly = true)
    public Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
}
