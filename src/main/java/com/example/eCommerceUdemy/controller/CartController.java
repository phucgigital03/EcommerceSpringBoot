package com.example.eCommerceUdemy.controller;


import com.example.eCommerceUdemy.model.Cart;
import com.example.eCommerceUdemy.payload.CartDTO;
import com.example.eCommerceUdemy.repository.CartItemRepository;
import com.example.eCommerceUdemy.repository.CartRepository;
import com.example.eCommerceUdemy.service.CartService;
import com.example.eCommerceUdemy.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {
    @Autowired
    CartService cartService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private AuthUtil authUtil;


    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> addProduct(@PathVariable Long productId,
                                              @PathVariable Integer quantity
    ) {
        CartDTO cartDTO = cartService.addProductToCart(productId,quantity);
        return new ResponseEntity<>(cartDTO, HttpStatus.CREATED);
    }

    @GetMapping("/carts")
    public ResponseEntity<List<CartDTO>> getAllCarts() {
        List<CartDTO> cartDTOList = cartService.getAllCarts();
        return new ResponseEntity<>(cartDTOList, HttpStatus.OK);
    }

    @GetMapping("/carts/user/cart")
    public ResponseEntity<CartDTO> getCartByUser() {
        String email = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(email);
        Long cartId = cart.getCartId();
        CartDTO cartDTO = cartService.getCart(email,cartId);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }




}
