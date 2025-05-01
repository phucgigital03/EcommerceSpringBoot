package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.MonthlyRevenueResponse;

import java.util.List;

public interface RevenueService {
    List<Integer> getAllAvailableYears();

    List<MonthlyRevenueResponse> getMonthlyRevenue(Integer year);
}
