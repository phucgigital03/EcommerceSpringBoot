package com.example.eCommerceUdemy.payload;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long productId;
    private String description;
    private String image;
    private String productName;
    private double discount;
    private double price;
    private double specialPrice;
    private int quantity;
    private long categoryId;
}
