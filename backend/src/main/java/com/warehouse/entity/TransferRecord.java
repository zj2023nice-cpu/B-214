package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transfer_records")
public class TransferRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serialNo;

    @ManyToOne
    @JoinColumn(name = "source_warehouse_id", nullable = false)
    private Warehouse sourceWarehouse;

    @ManyToOne
    @JoinColumn(name = "target_warehouse_id", nullable = false)
    private Warehouse targetWarehouse;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime transferTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferStatus status = TransferStatus.PENDING;

    private String remark;

    public enum TransferStatus {
        PENDING,
        COMPLETED,
        CANCELLED
    }
}
