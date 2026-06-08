package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDTO {
    private Long todayInboundTotal;
    private Long todayOutboundTotal;
    private Double monthlyStockValueChange;
}
