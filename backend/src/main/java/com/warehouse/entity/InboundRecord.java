package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inbound_records")
public class InboundRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serialNo; // 入库单号

    @Column(nullable = false)
    private LocalDateTime inboundTime;

    @Column(nullable = false)
    private Integer quantity;

    private Double price; // 入库单价

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier; // 冗余存储或关联，这里选择关联
    
    private String remark;
}
