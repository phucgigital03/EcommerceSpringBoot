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

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String email = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(email);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        if(product.getQuantity() == 0){
            throw new APIException("Product " + product.getProductName() + " has no quantity");
        }

        if(product.getQuantity() < quantity) {
            throw new APIException("Product " + product.getProductName() + " has no enough quantity");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);
        if(cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " doesn't exist in the cart");
        }

//      Calculating new quantity
        int newQuantity = cartItem.getQuantity() + quantity;
//      Validation to prevent negative quantities
        if(newQuantity < 0) {
            throw new APIException("The resulting quantity cannot be negative");
        }

        if(newQuantity == 0){
            deleteProductFromCart(cartId,productId);
        }else{
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice()*quantity));
            cartRepository.save(cart);
        }

//        CartItem updatedCartItem = cartItemRepository.save(cartItem);
//        if(updatedCartItem.getQuantity() == 0){
//            logger.debug("Quantity updated: " + updatedCartItem.getQuantity());
//            logger.debug("cartItemId: " + updatedCartItem.getCartItemId());
//            try{
//                cartItemRepository.deleteByCartItemId(updatedCartItem.getCartItemId());
//            }catch (Exception e){
//                logger.error("Error deleting cart item: ", e);
//            }
//        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cartItemRepository.findCartItemByCartId(cart.getCartId());
        logger.debug("length of cartItems: " + cartItems.size());
        Stream<ProductDTO> productDTOStream =
                cartItems.stream().map(item -> {
//                    logger.debug("productDTO: " + item.getProduct().getProductName());
                    ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
                    map.setQuantity(item.getQuantity());
                    return map;
                });
        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(()-> new ResourceNotFoundException("Cart","cartId",cartId));
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);
        if(cartItem == null) {
            throw new ResourceNotFoundException("Product","productId",productId);
        }
        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice()*cartItem.getQuantity()));

//        Product product = cartItem.getProduct();
//        product.setQuantity(product.getQuantity() + cartItem.getQuantity());

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId,productId);
        return "Product " + cartItem.getProduct().getProductName() + "  has been deleted";
    }

    @Transactional
    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        double cartPrice = cart.getTotalPrice()
                - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice
                + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepository.save(cartItem);
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
