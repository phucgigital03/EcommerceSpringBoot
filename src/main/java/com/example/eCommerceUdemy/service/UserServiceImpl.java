package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public boolean saveToken(String token, String refreshToken, String username) {
        User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException(username));
        user.setAccessToken(token);
        user.setRefreshToken(refreshToken);
        user.setIsTokenRevoked(false); // valid token
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean revokeToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        user.setAccessToken(null);
        user.setRefreshToken(null);
        user.setIsTokenRevoked(true); // valid token
        userRepository.save(user);
        return true;
    }


}
