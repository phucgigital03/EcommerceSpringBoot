package com.example.eCommerceUdemy.repository;

import com.example.eCommerceUdemy.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value =
            "SELECT oi.product_id, p.product_name, SUM(oi.quantity) as total_sold " +
                    "FROM order_item oi " +
                    "JOIN orders o ON oi.order_id = o.order_id " +
                    "JOIN products p ON oi.product_id = p.product_id " +
                    "WHERE o.order_date BETWEEN :startDate AND :endDate " +
                    "AND o.order_status = 'Order accepted' " +
                    "GROUP BY oi.product_id, p.product_name " +
                    "ORDER BY total_sold DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopSellingProductsInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit
    );

    @Query(value =
            "SELECT SUM(oi.quantity) " +
                    "FROM order_item oi " +
                    "JOIN orders o ON oi.order_id = o.order_id " +
                    "WHERE oi.product_id = :productId " +
                    "AND o.order_date BETWEEN :startDate AND :endDate " +
                    "AND o.order_status = 'Order accepted'",
            nativeQuery = true)
    Integer findTotalQuantitySoldByProductIdAndDateRange(
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
