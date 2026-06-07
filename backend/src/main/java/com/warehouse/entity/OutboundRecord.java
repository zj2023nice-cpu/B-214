package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "outbound_records")
public class OutboundRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serialNo; // 出库单号

    @Column(nullable = false)
    private LocalDateTime outboundTime;

    @Column(nullable = false)
    private Integer quantity;

    private Double price; // 出库单价（可能不同于入库价）

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    private String department; // 领用部门
    
    private String remark;
}
