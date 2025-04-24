package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.*;
import com.example.eCommerceUdemy.payload.*;
import com.example.eCommerceUdemy.repository.*;
import com.example.eCommerceUdemy.util.CurrencyConverterUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
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
    public void updateOrderVNPay(long orderId) {
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

        orderRepository.save(orderVNPayNotPaid);

        log.info("Order updated");
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
    public List<OrderDTO> getOrderByUser(String email) {
        System.out.println("Email: " + email);

        List<Order> orders =  orderRepository.findAllByEmail(email);

        for (Order order : orders) {
            System.out.println("Order: " + order);
        }

        return List.of();
    }
}
