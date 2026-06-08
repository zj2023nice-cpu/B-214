package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Formula;

@Data
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String contactPerson;
    private String phone;
    private String address;

    @Formula("(SELECT COALESCE(AVG(sr.rating), 0) FROM supplier_ratings sr WHERE sr.supplier_id = id)")
    private Double averageRating;
}
