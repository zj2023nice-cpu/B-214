package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "shelves")
public class Shelf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private Integer capacity;

    @Column(nullable = false)
    private Integer currentLoad = 0;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
}
