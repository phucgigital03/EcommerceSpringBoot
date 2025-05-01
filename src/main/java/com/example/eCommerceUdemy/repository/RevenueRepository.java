package com.example.eCommerceUdemy.repository;

import com.example.eCommerceUdemy.model.Order;
import com.example.eCommerceUdemy.payload.MonthlyRevenueResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevenueRepository extends JpaRepository<Order,Long> {
    @Query(value =
            "SELECT DISTINCT EXTRACT(YEAR FROM o.orderDate) " +
            "FROM Order o " +
            "ORDER BY EXTRACT(YEAR FROM o.orderDate) DESC"
    )
    List<Integer> findAllYears();

    @Query(value = """
                SELECT 
                    TO_CHAR(order_date, 'Mon') AS month,
                    ROUND(SUM(total_amount)::numeric, 1) AS revenue
                FROM orders
                WHERE EXTRACT(YEAR FROM order_date) = :year
                GROUP BY EXTRACT(MONTH FROM order_date), TO_CHAR(order_date, 'Mon')
                ORDER BY EXTRACT(MONTH FROM order_date)
            """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueByYear(@Param("year") int year);

}
