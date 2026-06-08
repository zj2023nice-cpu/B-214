package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "inventory_check_details")
public class InventoryCheckDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inventory_check_id", nullable = false)
    private InventoryCheck inventoryCheck;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Integer systemQuantity;

    private Integer actualQuantity;

    private Integer difference;

    private String remark;
}
