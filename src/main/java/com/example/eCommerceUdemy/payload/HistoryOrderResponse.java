package com.example.eCommerceUdemy.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryOrderResponse {
    private Long orderId;
    private String email;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate orderDate;
    private Double totalAmount;
    private String orderStatus;
    //  Relationship
    private List<HistoryOrderItem> orderItems;
    private PaymentDTO payment;
    private AddressDTO address;
}
