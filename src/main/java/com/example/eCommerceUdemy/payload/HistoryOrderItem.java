package com.example.eCommerceUdemy.payload;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryOrderItem {
    private Long orderItemId;
    private String productName;
    private String image;
    private Integer quantity;
    private double price;
    private double discount;
    private double orderedProductPrice;
}
