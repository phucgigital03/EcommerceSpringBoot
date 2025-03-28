package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.UsersResponse;

import java.util.List;

public interface UserService {
    boolean saveToken(String token,String refreshToken, String username);
    boolean revokeToken(String username);

    List<UsersResponse> findAllUsers();
}
