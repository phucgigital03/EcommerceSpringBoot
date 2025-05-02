package com.example.eCommerceUdemy.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SignUpMethodResponse {
    private String signUpMethod;
    private Long count;
}
