package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.UsersResponse;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserService {
    boolean saveToken(String token,String refreshToken, String username);
    boolean revokeToken(String username);

    List<UsersResponse> findAllUsers();

    Optional<User> findByEmail(String email);

    @Transactional
    void registerUser(User newUser);

    boolean saveAccessToken(String jwtToken, String username);

    GoogleAuthenticatorKey generate2FASecret(Long userId);

    boolean validate2FACode(Long userId, int code);

    void enable2FA(Long userId);

    void disable2FA(Long userId);
}
