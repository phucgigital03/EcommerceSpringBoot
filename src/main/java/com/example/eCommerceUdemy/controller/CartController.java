package com.example.eCommerceUdemy.controller;


import com.example.eCommerceUdemy.model.Cart;
import com.example.eCommerceUdemy.payload.CartDTO;
import com.example.eCommerceUdemy.payload.CartItemDTO;
import com.example.eCommerceUdemy.repository.CartItemRepository;
import com.example.eCommerceUdemy.repository.CartRepository;
import com.example.eCommerceUdemy.service.CartService;
import com.example.eCommerceUdemy.service.CartServiceImpl;
import com.example.eCommerceUdemy.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {
    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    CartService cartService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private AuthUtil authUtil;


    @PostMapping("/cart/test")
    public ResponseEntity<String> createOrUpdateCartTest(
            @RequestBody List<CartItemDTO> cartItemDTOS
    ) {
        logger.debug("createOrUpdateCart debug: {}", cartItemDTOS);
        Cart cart = cartRepository.findCartByEmail("u@gmail.com");
        System.out.println(cart);
        return new ResponseEntity<>("test API", HttpStatus.CREATED);
    }

    @PostMapping("/cart/create")
    public ResponseEntity<String> createOrUpdateCart(
            @RequestBody List<CartItemDTO> cartItemDTOS
    ) {
        logger.debug("createOrUpdateCart debug: {}", cartItemDTOS);
        String response = cartService.createOrUpdateCartWithItems(cartItemDTOS);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

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
        if(cart == null) {
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        Long cartId = cart.getCartId();
        CartDTO cartDTO = cartService.getCart(email,cartId);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @PutMapping("/carts/products/{productId}/quantity/{operation}")
    public ResponseEntity<CartDTO> updateProduct(@PathVariable Long productId,
                                              @PathVariable String operation
    ) {
        CartDTO cartDTO = cartService.updateProductQuantityInCart(productId,
                operation.equalsIgnoreCase("delete") ? -1 : 1);
        return new ResponseEntity<>(cartDTO, HttpStatus.CREATED);
    }

    @DeleteMapping("/carts/{cartId}/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(
            @PathVariable Long cartId,
            @PathVariable Long productId
    ){
        String status = cartService.deleteProductFromCart(cartId,productId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

}
