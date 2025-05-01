package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.config.AppConsants;
import com.example.eCommerceUdemy.payload.*;
import com.example.eCommerceUdemy.service.OrderService;
import com.example.eCommerceUdemy.service.RevenueService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private RevenueService revenueService;

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
    public ResponseEntity<List<HistoryOrderResponse>> getUserOrder() {
        String email = authUtil.loggedInEmail();
        List<HistoryOrderResponse> orderDTOs = orderService.getOrderByUser(email);
        return new ResponseEntity<>(orderDTOs,HttpStatus.OK);
    }


    @GetMapping("/order/admin/orders")
    public ResponseEntity<HistoryOrderPageResponse> getAllOrders(
            @RequestParam(name = "pageNumber",defaultValue = AppConsants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize",defaultValue = AppConsants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy",defaultValue = AppConsants.SORT_ORDERID_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder",defaultValue = AppConsants.SORT_ORDER, required = false) String sortOrder
    ) {
//        String email = authUtil.loggedInEmail();
        HistoryOrderPageResponse historyOrderPageResponse = orderService.getAllOrders(pageNumber,pageSize,sortBy,sortOrder);
        return new ResponseEntity<>(historyOrderPageResponse,HttpStatus.OK);
    }

    @GetMapping("/public/orders/count")
    public ResponseEntity<?> getOrderCount() {
        Long orderCount = orderService.getAllOrderCount();
        Map<String, Long> map = new HashMap<>();
        map.put("orderCount", orderCount);
        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    @GetMapping("/order/admin/revenue")
    public ResponseEntity<?> getOrdersRevenue() {
        Double orderRevenue = orderService.getRevenue();
        Long orderCount = orderService.getAllOrderCount();
        Map<String, Double> map = new HashMap<>();
        map.put("orderRevenue", orderRevenue);
        map.put("orderCount", Double.valueOf(orderCount));
        return new ResponseEntity<>(map,HttpStatus.OK);
    }

    @GetMapping("/public/order/availableYears")
    public ResponseEntity<?> getOrdersAvailableYears() {
        List<Integer> availableYears = revenueService.getAllAvailableYears();
        return new ResponseEntity<>(availableYears,HttpStatus.OK);
    }

    @GetMapping("/order/revenue/monthly")
    public List<MonthlyRevenueResponse> getMonthlyRevenue(
            @RequestParam(required = false) Integer year
    ) {
        return revenueService.getMonthlyRevenue(year);
    }

}
