package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierRatingDTO {
    private Long id;
    private Integer rating;
    private String content;
    private LocalDateTime ratingTime;
    private Long supplierId;
    private String supplierName;
    private Long userId;
    private String username;
}
