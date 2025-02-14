package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.payload.OrderDTO;
import com.example.eCommerceUdemy.payload.OrderRequestDTO;
import com.example.eCommerceUdemy.service.OrderService;
import com.example.eCommerceUdemy.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {
    @Autowired
    AuthUtil authUtil;
    @Autowired
    OrderService orderService;

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

}
