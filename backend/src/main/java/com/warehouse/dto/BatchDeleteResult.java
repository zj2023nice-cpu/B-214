package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchDeleteResult {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private boolean allSucceeded;
    private boolean rolledBack;
    private String summaryMessage;
    private List<BatchDeleteItemResult> items = new ArrayList<>();
    private List<BatchDeleteFailureDetail> failures = new ArrayList<>();
}
