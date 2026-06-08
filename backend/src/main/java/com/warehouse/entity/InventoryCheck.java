package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "inventory_checks")
public class InventoryCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serialNo;

    @Column(nullable = false)
    private LocalDateTime checkTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CheckStatus status = CheckStatus.PENDING;

    private String remark;

    @OneToMany(mappedBy = "inventoryCheck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InventoryCheckDetail> details;

    public enum CheckStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED
    }
}
