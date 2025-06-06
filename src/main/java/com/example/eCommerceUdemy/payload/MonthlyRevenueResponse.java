package com.example.eCommerceUdemy.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MonthlyRevenueResponse {
    private String month;
    private BigDecimal revenue;
}
