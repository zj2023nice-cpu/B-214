package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TurnoverRateDTO {
    private String code;
    private String name;
    private Long outboundTotal;
    private Double avgStock;
    private Double turnoverRate;
}
