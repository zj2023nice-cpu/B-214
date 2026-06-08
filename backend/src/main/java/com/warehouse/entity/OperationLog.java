package com.warehouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "operation_time")
    private LocalDateTime operationTime;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "operation_type", length = 30)
    private String operationType;

    @Column(name = "operation_target", length = 100)
    private String operationTarget;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "result", length = 20)
    private String result;

    @Column(name = "method", length = 200)
    private String method;

    @Column(name = "params", length = 1000)
    private String params;

    @Column(name = "error_msg", length = 500)
    private String errorMsg;
}
