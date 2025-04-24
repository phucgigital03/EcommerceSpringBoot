package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.payload.*;
import com.example.eCommerceUdemy.service.OrderService;
import com.example.eCommerceUdemy.service.StripeService;
import com.example.eCommerceUdemy.service.VNPayService;
import com.example.eCommerceUdemy.util.AuthUtil;
import com.example.eCommerceUdemy.util.RequestUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class OrderController {
    @Autowired
    AuthUtil authUtil;
    @Autowired
    OrderService orderService;
    @Autowired
    StripeService stripeService;

    @PostMapping("/order/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(
            @PathVariable("paymentMethod") String paymentMethod,
            @RequestBody OrderRequestDTO orderRequestDTO
    ) {
        String emailId = authUtil.loggedInEmail();
        OrderDTO order = orderService.placeOrder(
                emailId,
                orderRequestDTO.getAddressId(),
                paymentMethod,
                orderRequestDTO.getPgName(),
                orderRequestDTO.getPgPaymentId(),
                orderRequestDTO.getPgStatus(),
                orderRequestDTO.getPgResponseMessage()
        );
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @PostMapping("/order/stripe-client-secret")
    public ResponseEntity<String> createClientSecretStripe(
            @RequestBody StripePaymentDTO stripePaymentDTO
    ) throws StripeException {
        PaymentIntent paymentIntent = stripeService.paymentIntent(stripePaymentDTO);
        return new ResponseEntity<>(paymentIntent.getClientSecret(), HttpStatus.CREATED);
    }

    @PostMapping("/order/users/payments/vn-pay/{paymentMethod}")
    public ResponseEntity<?> orderProductsVNPay(
            @PathVariable("paymentMethod") String paymentMethod,
            @RequestBody InitPaymentRequest initPaymentRequestDTO,
            HttpServletRequest httpServletRequest
    ) {
        System.out.println("Test API: /order/users/payments/vn-pay/{paymentMethod}");
        var ipAddress = RequestUtil.getIpAddress(httpServletRequest);
        String emailId = authUtil.loggedInEmail();
        initPaymentRequestDTO.setIpAddress(ipAddress);

        OrderVNPayResDTO orderVNPayResDTO = orderService.placeOrderVNPay(emailId,paymentMethod,initPaymentRequestDTO);
        return new ResponseEntity<>(orderVNPayResDTO, HttpStatus.CREATED);
    }

    @GetMapping("/order/{orderId}/status")
    public ResponseEntity<OrderDTO> getOrderStatus(@PathVariable("orderId") Long orderId) {
        OrderDTO orderDTO = orderService.getOrderById(orderId);
        return new ResponseEntity<>(orderDTO,HttpStatus.OK);
    }

    @GetMapping("/order/user/orders")
    public ResponseEntity<List<OrderDTO>> getUserOrder() {
        String email = authUtil.loggedInEmail();
        List<OrderDTO> orderDTOs = orderService.getOrderByUser(email);
        return new ResponseEntity<>(orderDTOs,HttpStatus.OK);
    }
}
