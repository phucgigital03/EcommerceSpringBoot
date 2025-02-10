package com.example.eCommerceUdemy.security.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogInRequest {
    private String username;
    private String password;
}
