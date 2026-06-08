package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "materials")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String spec;
    private String unit;
    private Double price;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    private Integer alertThreshold = 10;

    @Column(name = "images", length = 2000)
    private String images;

    @Column(name = "alert_sent")
    private Boolean alertSent = false;

    @Transient
    public java.util.List<String> getImageList() {
        if (images == null || images.isBlank()) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    public void setImageList(java.util.List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.images = null;
        } else {
            this.images = String.join(",", imageList);
        }
    }
}
