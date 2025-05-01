package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.payload.MonthlyRevenueResponse;
import com.example.eCommerceUdemy.repository.RevenueRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
public class RevenueServiceImpl implements RevenueService {

    private final RevenueRepository revenueRepository;

    public RevenueServiceImpl(RevenueRepository revenueRepository) {
        this.revenueRepository = revenueRepository;
    }

    @Override
    public List<Integer> getAllAvailableYears() {
        try{
            return revenueRepository.findAllYears();
        } catch (Exception e) {
            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,"Something went wrong");
        }
    }

    @Override
    public List<MonthlyRevenueResponse> getMonthlyRevenue(Integer year) {

        try{
            // If no year is provided, use current year
            int yearToUse = year != null ? year : Year.now().getValue();
            List<Object[]> resultMonthlyRevenues = revenueRepository.findMonthlyRevenueByYear(yearToUse);
            List<MonthlyRevenueResponse> monthlyRevenue;
            monthlyRevenue = resultMonthlyRevenues.stream().map(item -> new MonthlyRevenueResponse(
                    (String) item[0],
                    (BigDecimal) item[1]
            )).toList();
            return monthlyRevenue;
        } catch (Exception e) {
            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,"Something went wrong");
        }
    }
}
