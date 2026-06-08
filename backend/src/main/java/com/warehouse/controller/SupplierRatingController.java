package com.warehouse.controller;

import com.warehouse.annotation.RequireRole;
import com.warehouse.common.ApiResponse;
import com.warehouse.dto.SupplierRatingDTO;
import com.warehouse.dto.SupplierWithRatingDTO;
import com.warehouse.service.SupplierRatingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier-ratings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierRatingController {

    private final SupplierRatingService ratingService;

    @GetMapping("/suppliers")
    public ApiResponse<List<SupplierWithRatingDTO>> getAllSuppliersWithRating() {
        return ApiResponse.success(ratingService.getAllSuppliersWithRating());
    }

    @PostMapping
    @RequireRole({"ADMIN", "USER"})
    public ApiResponse<SupplierRatingDTO> createRating(
            @RequestBody CreateRatingRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        SupplierRatingDTO dto = ratingService.createRating(
                userId, request.getSupplierId(), request.getRating(), request.getContent());
        return ApiResponse.success(dto);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    public ApiResponse<SupplierRatingDTO> updateRating(
            @PathVariable Long id,
            @RequestBody UpdateRatingRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        SupplierRatingDTO dto = ratingService.updateRating(
                userId, request.getSupplierId(), request.getRating(), request.getContent());
        return ApiResponse.success(dto);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "USER"})
    public ApiResponse<Void> deleteRating(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        ratingService.deleteRating(id, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/supplier/{supplierId}")
    public ApiResponse<List<SupplierRatingDTO>> getRatingsBySupplier(@PathVariable Long supplierId) {
        return ApiResponse.success(ratingService.getRatingsBySupplier(supplierId));
    }

    @GetMapping("/supplier/{supplierId}/average")
    public ApiResponse<Double> getAverageRating(@PathVariable Long supplierId) {
        return ApiResponse.success(ratingService.getAverageRating(supplierId));
    }

    @GetMapping("/supplier/{supplierId}/has-rated")
    @RequireRole({"ADMIN", "USER"})
    public ApiResponse<Boolean> hasUserRated(
            @PathVariable Long supplierId,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        return ApiResponse.success(ratingService.hasUserRated(userId, supplierId));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new RuntimeException("未提供用户身份信息");
        }
        return Long.parseLong(userIdHeader);
    }

    @Data
    public static class CreateRatingRequest {
        private Long supplierId;
        private Integer rating;
        private String content;
    }

    @Data
    public static class UpdateRatingRequest {
        private Long supplierId;
        private Integer rating;
        private String content;
    }
}
