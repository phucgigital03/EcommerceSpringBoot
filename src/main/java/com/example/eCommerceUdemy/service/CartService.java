package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.payload.CartDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface CartService {
    @Transactional
    CartDTO addProductToCart(Long productId, Integer quantity);

    List<CartDTO> getAllCarts();

    CartDTO getCart(String email,Long cartId);

    @Transactional
    CartDTO updateProductQuantityInCart(Long productId, Integer quantity);

    @Transactional
    String deleteProductFromCart(Long cartId, Long productId);

    @Transactional
    void updateProductInCarts(Long cartId, Long productId);
}
