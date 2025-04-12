package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.AppRole;
import com.example.eCommerceUdemy.model.Role;
import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.UsersResponse;
import com.example.eCommerceUdemy.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, TotpService totpService) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
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

    public boolean checkAdmin(Set<Role> roles){
        // Check if roles contain ROLE_ADMIN
        return roles.stream()
                .anyMatch(role -> role.getRoleName().equals(AppRole.ROLE_ADMIN));
    }

    @Override
    public List<UsersResponse> findAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("all","users","all");
        }
        users = users.stream()
                .filter(user -> {
                    Set<Role> roles = user.getRoles();
                    return !checkAdmin(roles);
                })
                .toList();

        List<UsersResponse> usersResponses = users.stream()
                .map(user -> modelMapper.map(user, UsersResponse.class))
                .collect(Collectors.toList());
        return usersResponses;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public void registerUser(User newUser) {
        if (newUser.getPassword() != null){
            newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        }
        userRepository.save(newUser);
    }

    @Override
    public boolean saveAccessToken(String jwtToken, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        user.setAccessToken(jwtToken);
        user.setIsTokenRevoked(false); // valid token
        userRepository.save(user);
        return true;
    }

    @Override
    public GoogleAuthenticatorKey generate2FASecret(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        GoogleAuthenticatorKey key = totpService.generateSecretKey();
        user.setTwoFactorSecret(key.getKey());
        userRepository.save(user);
        return key;
    }

    @Override
    public boolean validate2FACode(Long userId, int code){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return totpService.verifyQRCode(user.getTwoFactorSecret(), code);
    }

    @Override
    public void enable2FA(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void disable2FA(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
    }
}
