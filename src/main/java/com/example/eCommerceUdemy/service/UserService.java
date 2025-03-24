package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.model.User;

public interface UserService {
    boolean saveToken(String token,String refreshToken, String username);
    boolean revokeToken(String username);
}
