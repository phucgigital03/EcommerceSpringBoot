package com.example.eCommerceUdemy.controller;


import com.example.eCommerceUdemy.payload.VNPayIpnResponse;
import com.example.eCommerceUdemy.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/ipn")
    VNPayIpnResponse processIpn(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN: {}", params);

        return vnPayService.processIpn(params);
    }

}
