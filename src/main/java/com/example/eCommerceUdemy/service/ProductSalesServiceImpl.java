package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.repository.OrderItemRepository;
import com.example.eCommerceUdemy.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductSalesServiceImpl implements ProductSalesService {
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductSalesServiceImpl(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public List<Map<String, Object>> getTopSellingProductsByTimeRange(String timeRange) {
        LocalDate endDate = LocalDate.now();
        // Determine start date based on requested time range
        LocalDate startDate = switch (timeRange.toLowerCase()) {
            case "week" -> endDate.minusWeeks(1);
            case "year" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1);
        };
        return getTopSellingProductsByDateRange(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getTopSellingProductsByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            // 1. Get the top 10 selling products
            // based on total quantity sold in the date range
            // customize limit may be not 10
            List<Object[]> topProducts = orderItemRepository.findTopSellingProductsInDateRange(startDate, endDate, 10);

            List<Long> topProductIds = topProducts.stream()
                    .map(array -> (Long) array[0])
                    .toList();

            // 2. For each date in the range, get sales data for these top products
            List<Map<String, Object>> result = new ArrayList<>();

            // Determine the interval for data points (daily, weekly, or monthly)
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            ChronoUnit intervalUnit;
            int intervalStep;

            if (daysBetween <= 14) {
                // For short ranges, show daily data
                intervalUnit = ChronoUnit.DAYS;
                intervalStep = 1;
            } else if (daysBetween <= 90) {
                // For medium ranges, show weekly data
                intervalUnit = ChronoUnit.WEEKS;
                intervalStep = 1;
            } else if (daysBetween <= 120) {
                // For long ranges, show monthly data
                intervalUnit = ChronoUnit.MONTHS;
                intervalStep = 1;
            }else{
                throw new APIException(HttpStatus.NOT_ACCEPTABLE,"May be exceed 120 days");
            }

//            System.out.println("daysBetween: " + daysBetween);
//            System.out.println("intervalUnit: " + intervalUnit);
//            System.out.println("intervalStep: " + intervalStep);
//            topProductIds.forEach(id -> {
//                System.out.println("productId: " + id);
//            });

            // Generate data points based on the interval
            // segment with currentDate and intervalEndDate
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {

                LocalDate intervalEndDate = switch (intervalUnit) {
                    case WEEKS -> currentDate.plusWeeks(intervalStep).minusDays(1);
                    case MONTHS -> currentDate.plusMonths(intervalStep).minusDays(1);
                    default -> currentDate;
                };

                // Make sure we don't go past the overall end date
                if (intervalEndDate.isAfter(endDate)) {
                    intervalEndDate = endDate;
                }

                // Create data point
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", currentDate.toString());

                // Get sales data for each top product for this interval
                for (Long productId : topProductIds) {
                    Product product = productRepository.findById(productId).orElse(null);
                    if (product != null) {
                        Integer quantitySold = orderItemRepository.findTotalQuantitySoldByProductIdAndDateRange(
                                productId, currentDate, intervalEndDate);
                        dataPoint.put(product.getProductName(), quantitySold != null ? quantitySold : 0);
                    }
                }

                result.add(dataPoint);

                // Move to next interval
                currentDate = switch (intervalUnit) {
                    case WEEKS -> currentDate.plusWeeks(intervalStep);
                    case MONTHS -> currentDate.plusMonths(intervalStep);
                    default -> currentDate.plusDays(intervalStep);
                };

            }
//            System.out.println("result: " + result);
            return result;
        }catch (APIException e) {
            throw e; // re-throw as-is
        } catch (Exception e) {
            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
        }
    }

}
