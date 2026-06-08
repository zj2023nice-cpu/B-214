package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierWithRatingDTO {
    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String address;
    private Double averageRating;
    private Long ratingCount;
    private List<SupplierRatingDTO> latestRatings;
}
