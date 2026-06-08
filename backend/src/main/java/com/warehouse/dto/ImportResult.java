package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportResult {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<ImportFailureDetail> failures = new ArrayList<>();
}
