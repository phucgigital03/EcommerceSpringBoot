package com.example.eCommerceUdemy.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ProductSalesService {
    
    /**
     * Get top selling products by time range
     * 
     * @param timeRange - "week", "month", or "year"
     * @return List of data points with date and sales for top 10 products
     */
    List<Map<String, Object>> getTopSellingProductsByTimeRange(String timeRange);
    
    /**
     * Get top selling products by custom date range
     * 
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return List of data points with date and sales for top 10 products
     */
    List<Map<String, Object>> getTopSellingProductsByDateRange(LocalDate startDate, LocalDate endDate);
}