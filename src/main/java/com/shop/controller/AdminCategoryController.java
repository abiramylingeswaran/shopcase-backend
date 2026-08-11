package com.shop.controller;

import com.shop.dto.CategoryRequest;
import com.shop.dto.CategoryResponse;
import com.shop.service.CategoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * Multipart create — do not set {@code consumes = multipart/form-data}
     * (browsers/axios may append {@code charset=UTF-8}, which Spring then rejects).
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @RequestParam("name")
            @NotBlank
            @Size(min = 2, max = 100)
            String name,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request, image));
    }
}
