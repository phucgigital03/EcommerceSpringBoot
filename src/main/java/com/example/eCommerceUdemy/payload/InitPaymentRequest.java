package com.example.eCommerceUdemy.payload;

import lombok.Data;

@Data
public class InitPaymentRequest {
    private String ipAddress;

    private long amount; //Don't need pass value

    private Long addressId;
}
