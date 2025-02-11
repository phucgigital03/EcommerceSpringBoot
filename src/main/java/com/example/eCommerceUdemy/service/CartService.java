package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.CartDTO;

public interface CartService {
    CartDTO addProductToCart(Long productId, Integer quantity);
}
