package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyTrendDTO {
    private String date;
    private Long inboundQuantity;
    private Long outboundQuantity;
}
