package com.example.eCommerceUdemy.repository;

import com.example.eCommerceUdemy.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("select ci from CartItem ci where ci.cart.cartId = ?1 AND ci.product.productId = ?2")
    CartItem findCartItemByProductIdAndCartId(Long cartId, Long productId);

    @Query("select ci from CartItem ci where ci.cart.cartId = ?1")
    List<CartItem> findCartItemByCartId(Long cartId);
}
