package com.example.eCommerceUdemy.service;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public interface TotpService {
    GoogleAuthenticatorKey generateSecretKey();

    String getQRCodeUrl(GoogleAuthenticatorKey secretKey, String username);

    boolean verifyQRCode(String secretKey, int code);
}
