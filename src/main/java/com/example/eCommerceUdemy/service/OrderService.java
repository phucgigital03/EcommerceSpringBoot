package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.*;
import jakarta.transaction.Transactional;

import java.util.List;

public interface OrderService {
    @Transactional
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);

    @Transactional
    OrderVNPayResDTO placeOrderVNPay(String emailId, String paymentMethod, InitPaymentRequest initPaymentRequestDTO);

    @Transactional
    HistoryOrderResponse updateOrderVNPay(long orderId);

    OrderDTO getOrderById(Long orderId);

    List<HistoryOrderResponse> getOrderByUser(String email);

    HistoryOrderPageResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
}
