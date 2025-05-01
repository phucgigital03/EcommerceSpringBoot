package com.example.eCommerceUdemy.repository;

import com.example.eCommerceUdemy.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findAllByEmail(String email);
    boolean existsByShippingAddress_AddressId(Long addressId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    Double calculateTotalRevenue();
}
