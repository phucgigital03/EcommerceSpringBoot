package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.config.Symbol;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.Order;
import com.example.eCommerceUdemy.payload.InitPaymentRequest;
import com.example.eCommerceUdemy.payload.InitPaymentResponse;
import com.example.eCommerceUdemy.payload.VNPayIpnResponse;
import com.example.eCommerceUdemy.repository.OrderRepository;
import com.example.eCommerceUdemy.util.CurrencyConverterUtil;
import com.example.eCommerceUdemy.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
//@RequiredArgsConstructor
public class VNPayService implements PaymentService{
    public static final String VERSION = "2.1.0";
    public static final String COMMAND = "pay";
    public static final String ORDER_TYPE = "190000";
    public static final long DEFAULT_MULTIPLIER = 100L;
    @Autowired
    private CryptoService cryptoService;
    @Lazy
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;

    @Value("${payment.vnpay.tmn-code}")
    private String tmnCode;

    @Value("${payment.vnpay.init-payment-url}")
    private String initPaymentPrefixUrl;

    @Value("${payment.vnpay.return-url}")
    private String returnUrlFormat;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public InitPaymentResponse init(InitPaymentRequest request,String txnRefOrderId) {
//        var requestId = request.getRequestId();
        System.out.println("tmnCode: " + tmnCode);
        System.out.println("returnUrlFormat: " + frontendUrl + returnUrlFormat);
        var ipAddress = request.getIpAddress();
        var orderInfo = buildPaymentDetail(txnRefOrderId);
        var amount = request.getAmount() * DEFAULT_MULTIPLIER;
        var returnUrl = buildReturnUrl(txnRefOrderId);
        var vnCalendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        var createdDate = DateUtil.formatVnTime(vnCalendar);
        vnCalendar.add(Calendar.MINUTE, 15);
        var expiredDate = DateUtil.formatVnTime(vnCalendar);

        log.debug("createdDate: " + createdDate + " expiredDate: " + expiredDate);
        log.debug("ipAddress: " + ipAddress);

        Map<String, String> params = new HashMap<>();

        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", COMMAND);

        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRefOrderId);

        params.put("vnp_ReturnUrl", returnUrl);
//
        params.put("vnp_CreateDate", createdDate);
        params.put("vnp_ExpireDate", expiredDate);
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_Locale", "vn");
//
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", ORDER_TYPE);


        var initPaymentUrl = buildInitPaymentUrl(params);
//        System.out.println(initPaymentUrl);
        InitPaymentResponse initPaymentResponse = new InitPaymentResponse();
        initPaymentResponse.setVnpUrl(initPaymentUrl);
        return initPaymentResponse;
    }

    private String buildPaymentDetail(String txnRefOrderId) {
        return String.format("Thanh toan don hang %s", txnRefOrderId);
    }

    private String buildReturnUrl(String txnRef) {
        String returnUrl = frontendUrl + returnUrlFormat;
        return String.format(returnUrl, txnRef);
    }

    private String buildInitPaymentUrl(Map<String, String> params) {
        var hashPayload = new StringBuilder();
        var query = new StringBuilder();
        var fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        var itr = fieldNames.iterator();
        while (itr.hasNext()) {
            var fieldName = itr.next();
            var fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                //Build hash data
                hashPayload.append(fieldName);
                hashPayload.append(Symbol.EQUAL);
                hashPayload.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append(Symbol.EQUAL);
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append(Symbol.AND);
                    hashPayload.append(Symbol.AND);
                }
            }
        }

        var secureHash = cryptoService.sign(hashPayload.toString());

        query.append("&vnp_SecureHash=");
        query.append(secureHash);

        return initPaymentPrefixUrl + "?" + query;
    }

    public VNPayIpnResponse processIpn(Map<String, String> params) {
        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpAmount = params.get("vnp_Amount");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpTransactionStatus = params.get("vnp_TransactionStatus");

//      Verify checksum (signature validation)
        if (!verifyIpn(params)) {
            log.info("Invalid IPN verification");
            return new VNPayIpnResponse("97", "Signature Failed");
        }
//      check order by Id
        Order orderVNPayNotPaid = orderRepository.findById(Long.valueOf(vnpTxnRef))
                .orElse(null);
        if (orderVNPayNotPaid == null) {
            log.info("orderVNPayNotPaid is null");
            return new VNPayIpnResponse("01", "Order not found");
        }
//      check amount
        if(Long.parseLong(vnpAmount) != (CurrencyConverterUtil.convertUsdToVnd(orderVNPayNotPaid.getTotalAmount()) * 100)){
            log.info("orderVNPayNotPaid amount is " + orderVNPayNotPaid.getTotalAmount());
            return new VNPayIpnResponse("04", "Invalid Amount");
        }
//      check Order Status
        if(orderVNPayNotPaid.getOrderStatus().equalsIgnoreCase("Order accepted")){
            log.info("orderVNPayNotPaid has already been accepted");
            return new VNPayIpnResponse("02", "Order already paid");
        }
//      Validate transaction status
        if (!"00".equals(vnpTransactionStatus)) {
            log.info("order payment transaction failed");
            return new VNPayIpnResponse("02", "Transaction Failed");
        }

        VNPayIpnResponse response;
        try{
            var orderId = Long.parseLong(params.get("vnp_TxnRef"));
            // sending email inside orderService.updateOrderVNPay(orderId);
            orderService.updateOrderVNPay(orderId);
            response = new VNPayIpnResponse("00", "Successful");
        } catch (Exception e){
            response = new VNPayIpnResponse("99", "Unknown error");
        }
        return response;
    }

    private boolean verifyIpn(Map<String, String> params) {
        var reqSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        var hashPayload = new StringBuilder();
        var fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        var itr = fieldNames.iterator();
        while (itr.hasNext()) {
            var fieldName = itr.next();
            var fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                //Build hash data
                hashPayload.append(fieldName);
                hashPayload.append(Symbol.EQUAL);
                hashPayload.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    hashPayload.append(Symbol.AND);
                }
            }
        }

        var secureHash = cryptoService.sign(hashPayload.toString());
        return secureHash.equals(reqSecureHash);
    }
}
