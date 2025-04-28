package com.example.eCommerceUdemy.service;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.model.AppRole;
import com.example.eCommerceUdemy.model.Role;
import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.UsersResponse;
import com.example.eCommerceUdemy.repository.RoleRepository;
import com.example.eCommerceUdemy.repository.UserRepository;
import com.example.eCommerceUdemy.security.request.SignupRequest;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
    private final RoleRepository roleRepository;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, TotpService totpService, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.roleRepository = roleRepository;
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
        if(user.isTwoFactorEnabled()){
            throw new RuntimeException("Two-factor authentication is enabled");
        }

        String secret2FAKeyDB = user.getTwoFactorSecret();
        GoogleAuthenticatorKey key = null;
        if(secret2FAKeyDB == null || secret2FAKeyDB.isEmpty()){
            System.out.println("Secret2FAKeyDB doesn't exist");
            key = totpService.generateSecretKey();
            user.setTwoFactorSecret(key.getKey());
            userRepository.save(user);
        }else{
            System.out.println("Secret2FAKeyDB already exists");
            key = new GoogleAuthenticatorKey.Builder(secret2FAKeyDB).build();
        }
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
        user.setTwoFactorSecret(null);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
    }

    @Override
    public User getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("userId","user","userId"));
        return user;
    }

    @Override
    public User findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("userName","user","userName"));
        return user;
    }

    @Transactional
    @Override
    public String updatedUser(Long userId, SignupRequest signUpRequest) {
        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("userId","user","userId"));

        // update field without Role field
        if (!updatedUser.getUsername().equals(signUpRequest.getUsername())){
            boolean existedUsername = userRepository.existsByUsername(signUpRequest.getUsername());
            if(existedUsername){
                throw new APIException(HttpStatus.NOT_ACCEPTABLE,"Username already exists");
            }
            updatedUser.setUsername(signUpRequest.getUsername());
        }

        if(!updatedUser.getEmail().equals(signUpRequest.getEmail())){
            boolean existedEmail = userRepository.existsByEmail(signUpRequest.getEmail());
            if(existedEmail){
                throw new APIException(HttpStatus.NOT_ACCEPTABLE,"Email already exists");
            }
            updatedUser.setEmail(signUpRequest.getEmail());
        }

        // check user's roles from DB have different updated Role
        Set<String> dbRoleNames = updatedUser.getRoles().stream().map(
                    role -> role.getRoleName().toString())
                        .collect(Collectors.toSet());

        Set<String> requestedRoleNames = signUpRequest.getRole().stream().map(role -> {
            return switch (role) {
                case "admin" -> "ROLE_ADMIN";
                case "seller" -> "ROLE_SELLER";
                default -> "ROLE_USER";
            };
        }).collect(Collectors.toSet());

        System.out.println("dbRoleNames" + dbRoleNames);
        System.out.println("requestedRoleNames" + requestedRoleNames);

        boolean rolesAreDifferent = !dbRoleNames.equals(requestedRoleNames);
        if(rolesAreDifferent) {
            System.out.println("Roles are different");
            // create new Roles for user
            Set<Role> newRoles = new HashSet<>();
            requestedRoleNames.forEach(role -> {
                switch (role) {
                    case "ROLE_ADMIN":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        newRoles.add(adminRole);
                        break;
                    case "ROLE_SELLER":
                        Role modRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        newRoles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        newRoles.add(userRole);
                }
            });
            // update roles
            updatedUser.setRoles(newRoles);
        }else{
            System.out.println("Roles are not different");
        }

        userRepository.save(updatedUser);
        return "Updated user successfully with " + userId;
    }

    @Override
    public String deletedUser(Long userId) {
        System.out.println("deleting user" + userId);
        User deletedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("userId","user","userId"));

        // Use soft delete
        deletedUser.setDeleted(true);
        userRepository.save(deletedUser);
        return "Deleted user successfully with " + userId;
    }
}
