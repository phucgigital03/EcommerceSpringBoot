package com.example.eCommerceUdemy.controller;


import com.example.eCommerceUdemy.payload.CartDTO;
import com.example.eCommerceUdemy.service.CartService;
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

    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> addProduct(@PathVariable Long productId,
                                              @PathVariable Integer quantity
    ) {
        CartDTO cartDTO = cartService.addProductToCart(productId,quantity);
        return new ResponseEntity<>(cartDTO, HttpStatus.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<List<CartDTO>> getAllCarts() {
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

}
