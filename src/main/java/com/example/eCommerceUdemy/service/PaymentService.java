package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.InitPaymentRequest;
import com.example.eCommerceUdemy.payload.InitPaymentResponse;

public interface PaymentService {
    InitPaymentResponse init(InitPaymentRequest request,String txnRefOrderId);
}
