package com.example.eCommerceUdemy.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    private Double totalPrice = 0.0;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
//    @ToString.Exclude
//    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "cart",
            fetch = FetchType.EAGER,
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE,
                    CascadeType.REMOVE
            },
            orphanRemoval = true
    )
    @ToString.Exclude
    @JsonIgnore
    private List<CartItem> cartItems = new ArrayList<>();
}
