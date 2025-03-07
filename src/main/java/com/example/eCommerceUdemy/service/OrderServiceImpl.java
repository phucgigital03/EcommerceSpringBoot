package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.*;
import com.example.eCommerceUdemy.payload.OrderDTO;
import com.example.eCommerceUdemy.payload.OrderItemDTO;
import com.example.eCommerceUdemy.payload.ProductDTO;
import com.example.eCommerceUdemy.repository.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
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
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "emailId", emailId);
        }
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order accepted");
        order.setShippingAddress(address);

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

            Product product = orderItem.getProduct();

            product.setQuantity(product.getQuantity() - quantity);

            productRepository.save(product);

            cartService.deleteProductFromCart(cart.getCartId(),orderItem.getProduct().getProductId());

        });

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
}
