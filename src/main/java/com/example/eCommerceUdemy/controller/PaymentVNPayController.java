package com.example.eCommerceUdemy.controller;


import com.example.eCommerceUdemy.payload.HistoryOrderResponse;
import com.example.eCommerceUdemy.payload.VNPayIpnResponse;
import com.example.eCommerceUdemy.service.EmailService;
import com.example.eCommerceUdemy.service.VNPayService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/payment/vn-pay")
@Slf4j
public class PaymentVNPayController {
    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/ipn")
    VNPayIpnResponse processIpn(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN: {}", params);

        return vnPayService.processIpn(params);
    }

    @PostMapping("/sendOrderConfirmTest")
    ResponseEntity<?> sendOrderConfirmTest(@RequestBody HistoryOrderResponse historyOrderResponse) {
        System.out.println("History order " + historyOrderResponse);
        try {
            System.out.println("Order saved successfully. Sending confirmation email for order ID: " + historyOrderResponse.getOrderId());
            emailService.sendOrderConfirmationEmail(historyOrderResponse);
            System.out.println("Confirmation email sent successfully for order ID: " + historyOrderResponse.getOrderId());
            return ResponseEntity.status(HttpStatus.OK).body("Order sent successfully");
        } catch (MessagingException e) {
            System.out.println("Failed to send confirmation email for order ID: " + historyOrderResponse.getOrderId() + e);
            // We don't rethrow the exception to avoid rolling back the transaction
            // But you could implement a retry mechanism or queue the email for later
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}
