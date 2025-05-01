package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.*;
import com.example.eCommerceUdemy.payload.*;
import com.example.eCommerceUdemy.repository.*;
import com.example.eCommerceUdemy.util.ConstructImageUtil;
import com.example.eCommerceUdemy.util.CurrencyConverterUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    OrderItemRepository orderItemRepository;
    @Autowired
    AddressRepository addressRepository;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    CartService cartService;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    VNPayService vnPayService;
    @Autowired
    ConstructImageUtil constructImageUtil;
    @Autowired
    EmailService emailService;

    @Override
    @Transactional
    public OrderDTO placeOrder(
            String emailId,
            Long addressId,
            String paymentMethod,
            String pgName,
            String pgPaymentId,
            String pgStatus,
            String pgResponseMessage
    ) {
        //1.check cart exist
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "emailId", emailId);
        }
        //2.check address exist
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        // 3.create order object
        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order accepted");
        order.setShippingAddress(address);

        // create payment method before because order have FK (payment_id)
        Payment payment = new Payment(
                paymentMethod,
                pgPaymentId,
                pgStatus,
                pgResponseMessage,
                pgName
        );
        payment.setOrder(order);
        payment = paymentRepository.save(payment);

        order.setPayment(payment);
        //saved Order
        Order savedOrder = orderRepository.save(order);

        //4.check cartItems to save orderItems
        List<CartItem> cartItems = cart.getCartItems();
        if(cartItems.isEmpty()) {
            throw new APIException("cartItems is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }
        // 5.saved OrderItems
        orderItems = orderItemRepository.saveAll(orderItems);

        // 6.clean up cartItems and update product quantity
        cart.getCartItems().forEach(orderItem -> {
            int quantity = orderItem.getQuantity();

            Product product = orderItem.getProduct();

            product.setQuantity(product.getQuantity() - quantity);

            productRepository.save(product);

            cartService.deleteProductFromCart(cart.getCartId(),orderItem.getProduct().getProductId());

        });

        // 7. convert Order to OrderDTO
        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
        orderItems.forEach(orderItem -> {
            OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);

            ProductDTO productDTO = modelMapper.map(orderItem.getProduct(), ProductDTO.class);

            orderItemDTO.setProductDTO(productDTO);

            orderDTO.getOrderItems().add(orderItemDTO);
        });
        orderDTO.setAddressId(addressId);
        return orderDTO;
    }

    @Override
    @Transactional
    public OrderVNPayResDTO placeOrderVNPay(String emailId, String paymentMethod, InitPaymentRequest initPaymentRequestDTO) {
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "emailId", emailId);
        }
        Long addressId = initPaymentRequestDTO.getAddressId();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order pending");
        order.setShippingAddress(address);

        String pgName = "VNPay";
        String pgStatus = "Pending";
        String pgResponseMessage = "";
        String pgPaymentId = null;

        Payment payment = new Payment(
                paymentMethod,
                pgPaymentId,
                pgStatus,
                pgResponseMessage,
                pgName
        );
        payment.setOrder(order);
        payment = paymentRepository.save(payment);

        order.setPayment(payment);
        Order savedOrder = orderRepository.save(order);

        payment.setPgPaymentId(String.valueOf(order.getOrderId()));
        payment = paymentRepository.save(payment);
        savedOrder.setPayment(payment);

        List<CartItem> cartItems = cart.getCartItems();
        if(cartItems.isEmpty()) {
            throw new APIException("cartItems is empty");
        }
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }

        orderItems = orderItemRepository.saveAll(orderItems);

        cart.getCartItems().forEach(orderItem -> {
            int quantity = orderItem.getQuantity();

//            Product product = orderItem.getProduct();

//            product.setQuantity(product.getQuantity() - quantity);

//            productRepository.save(product);

            cartService.deleteProductFromCart(cart.getCartId(),orderItem.getProduct().getProductId());
        });

        OrderVNPayResDTO orderVNPayResDTO = modelMapper.map(savedOrder, OrderVNPayResDTO.class);
        orderItems.forEach(orderItem -> {
            OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);

            ProductDTO productDTO = modelMapper.map(orderItem.getProduct(), ProductDTO.class);

            orderItemDTO.setProductDTO(productDTO);

            orderVNPayResDTO.getOrderItems().add(orderItemDTO);
        });
        orderVNPayResDTO.setAddressId(addressId);

        initPaymentRequestDTO.setAmount(CurrencyConverterUtil.convertUsdToVnd(order.getTotalAmount()));
        InitPaymentResponse initPaymentResponseDTO = vnPayService.init(initPaymentRequestDTO,String.valueOf(order.getOrderId()));
        orderVNPayResDTO.setVnPayRes(initPaymentResponseDTO);
        return orderVNPayResDTO;
    }

    @Override
    @Transactional
    public HistoryOrderResponse updateOrderVNPay(long orderId) {
        Order orderVNPayNotPaid = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        orderVNPayNotPaid.setOrderStatus("Order accepted");

        List<OrderItem> orderItems = orderVNPayNotPaid.getOrderItems();
        orderItems.forEach(orderItem -> {
            int quantity = orderItem.getQuantity();

            Product product = orderItem.getProduct();

            product.setQuantity(product.getQuantity() - quantity);

            productRepository.save(product);
        });

        Payment payment = orderVNPayNotPaid.getPayment();
        payment.setPgStatus("succeeded");
        payment.setPgResponseMessage("Payment successful");
        paymentRepository.save(payment);

        Order updatedOrder =  orderRepository.save(orderVNPayNotPaid);
        log.info("Order updated");

        // Convert Order to HistoryOrderResponse:
        HistoryOrderResponse historyOrderResponse = modelMapper.map(updatedOrder, HistoryOrderResponse.class);
        List<HistoryOrderItem> historyOrderItems = new ArrayList<>();
        // 1.Set historyOrderItem base on orderItem,product
        // and then add to historyOrderItems
        updatedOrder.getOrderItems().forEach(orderItem -> {
            HistoryOrderItem historyOrderItem = new HistoryOrderItem();
            historyOrderItem.setOrderItemId(orderItem.getOrderItemId());
            historyOrderItem.setPrice(orderItem.getProduct().getPrice());
            historyOrderItem.setQuantity(orderItem.getQuantity());
            historyOrderItem.setOrderedProductPrice(orderItem.getOrderedProductPrice());
            historyOrderItem.setImage(constructImageUtil.constructImage(orderItem.getProduct().getImage()));
            historyOrderItem.setDiscount(orderItem.getDiscount());
            historyOrderItem.setProductName(orderItem.getProduct().getProductName());
            historyOrderItems.add(historyOrderItem);
        });
        historyOrderResponse.setOrderItems(historyOrderItems);

        // 2.Set username in AddressDTO
        String username = updatedOrder.getShippingAddress().getUser().getUsername();
        historyOrderResponse.getAddress().setUsername(username);

        // Send Order Confirm Here
        emailService.sendOrderConfirmationEmail(historyOrderResponse);

        return historyOrderResponse;
    }

    @Override
    public OrderDTO getOrderById(Long orderId) {
        Order orderDB = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderDTO orderDTO = modelMapper.map(orderDB, OrderDTO.class);
        orderDB.getOrderItems().forEach(orderItem -> {
            OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);
            orderDTO.getOrderItems().remove(orderItemDTO);

            ProductDTO productDTO = modelMapper.map(orderItem.getProduct(), ProductDTO.class);

            orderItemDTO.setProductDTO(productDTO);

            orderDTO.getOrderItems().add(orderItemDTO);
        });
        return orderDTO;
    }

    @Override
    public List<HistoryOrderResponse> getOrderByUser(String email) {
        System.out.println("Email: " + email);
        List<HistoryOrderResponse> historyOrderResponses = new ArrayList<>();
        List<Order> orders =  orderRepository.findAllByEmail(email);

        //convert order to historyOrderResponse
        getHistoryOrderResponses(orders, historyOrderResponses);
        return historyOrderResponses;
    }

    @Override
    public HistoryOrderPageResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        System.out.println("pageNumber: " + pageNumber);
        System.out.println("pageSize: " + pageSize);
        System.out.println("sortBy: " + sortBy);
        System.out.println("sortOrder: " + sortOrder);

        //      sortBy and sortOrder
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        //      Specification is part Spring Data JPA
        Specification<Order> spec = Specification.where(null);
        Page<Order> orderPage = orderRepository.findAll(spec,pageable);
        //      Get Orders
        List<Order> orders = orderPage.getContent();
        if(orders.isEmpty()){
            throw new APIException("No orders found");
        }

        List<HistoryOrderResponse> historyOrderResponses = new ArrayList<>();
        //convert order to historyOrderResponse
        getHistoryOrderResponses(orders, historyOrderResponses);

        HistoryOrderPageResponse historyOrderPageResponse = new HistoryOrderPageResponse();
        historyOrderPageResponse.setContent(historyOrderResponses);
        historyOrderPageResponse.setPageNumber(orderPage.getNumber());
        historyOrderPageResponse.setPageSize(orderPage.getSize());
        historyOrderPageResponse.setTotalPages(orderPage.getTotalPages());
        historyOrderPageResponse.setTotalElements(orderPage.getNumberOfElements());
        historyOrderPageResponse.setLastPage(orderPage.isLast());
        return historyOrderPageResponse;
    }

    @Override
    public Long getAllOrderCount() {
        return orderRepository.count();
    }

    @Override
    public Double getRevenue() {
        try {
            return orderRepository.calculateTotalRevenue();
        } catch (Exception e) {
            throw new APIException(HttpStatus.NOT_ACCEPTABLE,"Error getting revenue");
        }
    }

    private void getHistoryOrderResponses(List<Order> orders, List<HistoryOrderResponse> historyOrderResponses) {
        for (Order order : orders) {
            HistoryOrderResponse historyOrderResponse = modelMapper.map(order, HistoryOrderResponse.class);
            List<HistoryOrderItem> historyOrderItems = new ArrayList<>();
//            System.out.println("order.getOrderItems(): " + order.getOrderItems());

            //set historyOrderItem base on orderItem,product
            // and then add to historyOrderItems
            order.getOrderItems().forEach(orderItem -> {
                HistoryOrderItem historyOrderItem = new HistoryOrderItem();

                historyOrderItem.setOrderItemId(orderItem.getOrderItemId());
                historyOrderItem.setPrice(orderItem.getProduct().getPrice());
                historyOrderItem.setQuantity(orderItem.getQuantity());
                historyOrderItem.setOrderedProductPrice(orderItem.getOrderedProductPrice());
                historyOrderItem.setImage(constructImageUtil.constructImage(orderItem.getProduct().getImage()));
                historyOrderItem.setDiscount(orderItem.getDiscount());
                historyOrderItem.setProductName(orderItem.getProduct().getProductName());

                historyOrderItems.add(historyOrderItem);
            });

            historyOrderResponse.setOrderItems(historyOrderItems);

            //add historyOrderResponse to List
            historyOrderResponses.add(historyOrderResponse);
        }
    }

}
