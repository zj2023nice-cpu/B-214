package com.warehouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "supplier_ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "supplier_id"})
})
public class SupplierRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime ratingTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
