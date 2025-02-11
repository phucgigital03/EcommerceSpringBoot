package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.Cart;
import com.example.eCommerceUdemy.model.CartItem;
import com.example.eCommerceUdemy.model.Product;
import com.example.eCommerceUdemy.payload.CartDTO;
import com.example.eCommerceUdemy.payload.ProductDTO;
import com.example.eCommerceUdemy.repository.CartItemRepository;
import com.example.eCommerceUdemy.repository.CartRepository;
import com.example.eCommerceUdemy.repository.ProductRepository;
import com.example.eCommerceUdemy.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
//      Find existing cart or create cart
        Cart cart = createCart();
//      Retrieve Product Details
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

//      Perform Validations
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(),productId);
        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }
        if(product.getQuantity() == 0){
            throw new APIException("Product " + product.getProductName() + " has no quantity");
        }
        if(product.getQuantity() < quantity) {
            throw new APIException("Product " + product.getProductName() + " has no enough quantity");
        }

//      Create CartItem
        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());
//      Save cart item
        cartItemRepository.save(newCartItem);
//      update when save cart item
        product.setQuantity(product.getQuantity());
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice()*quantity));
//      cart.getCartItems().add(newCartItem);
        cartRepository.save(cart);
//      convert cart to cartDTO
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cartItemRepository.findCartItemByCartId(cart.getCartId());
//        List<CartItem> cartItems = cart.getCartItems();
        logger.debug("length of cartItems: " + cartItems.size());
        Stream<ProductDTO> productDTOStream =
                cartItems.stream().map(item -> {
//                    logger.debug("productDTO: " + item.getProduct().getProductName());
                    ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
                    map.setQuantity(item.getQuantity());
                    return map;
                });
        cartDTO.setProducts(productDTOStream.toList());
//      return updated cart
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if(carts.size() == 0) {
            throw new APIException("No carts found");
        }

        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> productDTOS = cart.getCartItems().stream().map(
                    cartItem -> {
                        ProductDTO proDTO =  modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                        proDTO.setQuantity(cartItem.getQuantity());
                        return proDTO;
                    }
            ).toList();

            cartDTO.setProducts(productDTOS);
            return cartDTO;
        }).toList();
        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String email, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(email,cartId);
        if(cart == null) {
            throw new ResourceNotFoundException("Cart","cartId",cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<ProductDTO> productDTOS = cart.getCartItems().stream()
                .map(cartItem -> {
                    ProductDTO proDTO =  modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                    proDTO.setQuantity(cartItem.getQuantity());
                    return proDTO;
                }).toList();
        cartDTO.setProducts(productDTOS);
        return cartDTO;
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) {
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }
}
