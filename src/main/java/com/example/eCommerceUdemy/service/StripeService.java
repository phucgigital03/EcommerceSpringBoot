package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.StripePaymentDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Service;


public interface StripeService {
    PaymentIntent paymentIntent(StripePaymentDTO stripePaymentDTO) throws StripeException;
}
