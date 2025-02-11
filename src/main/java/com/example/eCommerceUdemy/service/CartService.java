package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.CartDTO;

import java.util.List;

public interface CartService {
    CartDTO addProductToCart(Long productId, Integer quantity);

    List<CartDTO> getAllCarts();

    CartDTO getCart(String email,Long cartId);
}
