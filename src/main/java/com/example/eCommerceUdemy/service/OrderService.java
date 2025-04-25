package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.HistoryOrderResponse;
import com.example.eCommerceUdemy.payload.InitPaymentRequest;
import com.example.eCommerceUdemy.payload.OrderDTO;
import com.example.eCommerceUdemy.payload.OrderVNPayResDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface OrderService {
    @Transactional
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);

    @Transactional
    OrderVNPayResDTO placeOrderVNPay(String emailId, String paymentMethod, InitPaymentRequest initPaymentRequestDTO);

    @Transactional
    void updateOrderVNPay(long orderId);

    OrderDTO getOrderById(Long orderId);

    List<HistoryOrderResponse> getOrderByUser(String email);
}
