package com.example.eCommerceUdemy.controller;

import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.ResourceNotFoundException;
import com.example.eCommerceUdemy.exception.TokenExpireException;
import com.example.eCommerceUdemy.model.AppRole;
import com.example.eCommerceUdemy.model.Role;
import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.UserTokenDTO;
import com.example.eCommerceUdemy.repository.RoleRepository;
import com.example.eCommerceUdemy.repository.UserRepository;
import com.example.eCommerceUdemy.security.jwt.JwtUtils;
import com.example.eCommerceUdemy.security.request.LogInRequest;
import com.example.eCommerceUdemy.security.request.SignupRequest;
import com.example.eCommerceUdemy.security.response.MessageResponse;
import com.example.eCommerceUdemy.security.response.RefreshTokenResponse;
import com.example.eCommerceUdemy.security.response.UserInfoResponse;
import com.example.eCommerceUdemy.security.service.UserDetailsImpl;
import com.example.eCommerceUdemy.service.TotpService;
import com.example.eCommerceUdemy.service.UserService;
import com.example.eCommerceUdemy.service.UserServiceImpl;
import com.example.eCommerceUdemy.util.AuthUtil;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private TotpService totpService;

    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody LogInRequest logInRequest) {
        Authentication authentication;
        try {
            logger.debug("check username and password at controller");
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            logInRequest.getUsername(),
                            logInRequest.getPassword()
                    ));
        } catch (AuthenticationException e) {
            logger.debug("AuthenticationException at controller " + e.getMessage());
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
        }
        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
        ResponseCookie jwtRefreshCookie = jwtUtils.generateJwtRefreshCookie(userDetails);

//      save accessToken and refreshToken to DB
        userServiceImpl.saveToken(jwtToken, jwtUtils.extractJwtFromResponseCookie(jwtRefreshCookie.toString()), userDetails.getUsername());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        UserInfoResponse response =
                new UserInfoResponse(
                        userDetails.getId(),
                        jwtToken,
                        userDetails.getUsername(),
                        roles
                );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                .body(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername((signUpRequest.getUsername()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail((signUpRequest.getEmail()))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "seller":
                        Role modRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/username")
    public String getUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        } else {
            return "NULL";
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        UserInfoResponse response =
                new UserInfoResponse(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        roles
                );
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signOut(HttpServletRequest request) {
//      extract refresh token from cookie
        String refreshToken = jwtUtils.getJwtFromCookie(request);
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Refresh token is empty!"));
        }
        logger.debug("refresh token: " + refreshToken);
        try {
//          extract username from refresh token
            String username = jwtUtils.getUsernameFromToken(refreshToken);

//          Find user from DB
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("username", "User", username));

//          revoke all token
            userServiceImpl.revokeToken(user.getUsername());
            ResponseCookie jwtCookie = jwtUtils.getCleanJwtCookie();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(new MessageResponse("Signed out successfully!"));
        } catch (Exception e) {
            logger.error("Invalid refresh token : ", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid refresh token! Need to login"));
        }
    }

    @GetMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        System.out.println("Refresh token");
//      extract refresh token from cookie
        String refreshToken = jwtUtils.getJwtFromCookie(request);
        if (refreshToken == null) {
            return new ResponseEntity<>(new MessageResponse("Refresh token is empty!"), HttpStatus.BAD_REQUEST);
        }
        try {
//          extract username from token
            String username = jwtUtils.getUsernameFromToken(refreshToken);
//          check if user exists in DB
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User is not found!"));
//          check refresh token is valid
            String refreshTokenDB = user.getRefreshToken();
            logger.debug("Refresh token DB: " + refreshTokenDB);
            if (jwtUtils.validateToken(refreshTokenDB)) {
                //      generate accessToken and old refreshToken
                String accessToken = jwtUtils.generateTokenFromUsername(UserDetailsImpl.build(user));
                logger.debug("Refresh token DB to generate new accessToken: " + accessToken);
                //      revoke or save accessToken to DB
                userServiceImpl.saveToken(accessToken, refreshTokenDB, user.getUsername());
                return new ResponseEntity<>(new RefreshTokenResponse(accessToken, refreshTokenDB), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new MessageResponse("Refresh token is invalid!"), HttpStatus.BAD_REQUEST);
            }
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT refresh token: {}", e.getMessage());
            throw new APIException(HttpStatus.BAD_REQUEST, "Invalid JWT refresh token");
        } catch (ExpiredJwtException e) {
            logger.error("JWT refresh token is expired: {}", e.getMessage());
            throw new APIException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT refresh token is unsupported: {}", e.getMessage());
            throw new APIException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT refresh token claims string is empty: {}", e.getMessage());
            throw new APIException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/oauth2/set-cookie")
    public ResponseEntity<?> setCookie(@RequestBody UserTokenDTO userTokenDTO) {
        try {
            System.out.println("userTokenDTO: " + userTokenDTO);
//      Check user exists in DB
            User user = userRepository.findById(userTokenDTO.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userTokenDTO.getUserId()));
//      Check token equals with user's token from DB
            if (user.getAccessToken() == null) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Access token is empty!");
            }

            if (!user.getAccessToken().equals(userTokenDTO.getToken())) {
                throw new APIException(HttpStatus.UNAUTHORIZED, "Access token don't match");
            }
//      Check valid token
            if (!jwtUtils.validateToken(user.getAccessToken())) {
                throw new APIException(HttpStatus.UNAUTHORIZED, "Invalid access token");
            }
//      Check refreshToken exists in DB if refreshToken equals null,
//      allowing to setCookie
            if (user.getRefreshToken() != null) {
                throw new APIException(HttpStatus.UNAUTHORIZED, "Refresh token already exists, not setting again!");
            }

//      Generate JWT RefreshToken
            ResponseCookie jwtRefreshCookie = jwtUtils.generateJwtRefreshCookie(UserDetailsImpl.build(user));

//      save accessToken and refreshToken to DB
            userServiceImpl.saveToken(
                    user.getAccessToken(),
                    jwtUtils.extractJwtFromResponseCookie(jwtRefreshCookie.toString()),
                    user.getUsername()
            );

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getRoleName().name())
                    .toList();

            UserInfoResponse response =
                    new UserInfoResponse(
                            user.getUserId(),
                            user.getAccessToken(),
                            user.getUsername(),
                            roles
                    );

//      Add the cookie to the response
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                    .body(response);
        } catch (TokenExpireException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid access token or expired access token!"));
        }
    }

    //  For 2FA authentication
    @PostMapping("/enable-2fa")
    public ResponseEntity<String> enable2FA() {
        try {
            Long userId = authUtil.loggedInUserId();
            GoogleAuthenticatorKey secret = userService.generate2FASecret(userId);
            String qrCodeUrl = totpService.getQRCodeUrl(secret,
                    userService.getUserById(userId).getUsername());
            return ResponseEntity.ok(qrCodeUrl);
        } catch (Exception e) {
            System.out.println("Enable2FA Error: " + e.getMessage());
            return ResponseEntity.status(400).body("Bad Request");
        }
    }

    @PostMapping("/disable-2fa")
    public ResponseEntity<String> disable2FA() {
        Long userId = authUtil.loggedInUserId();
        userService.disable2FA(userId);
        return ResponseEntity.ok("2FA disabled");
    }


    @PostMapping("/verify-2fa")
    public ResponseEntity<String> verify2FA(@RequestParam int code) {
        Long userId = authUtil.loggedInUserId();
        boolean isValid = userService.validate2FACode(userId, code);
        if (isValid) {
            userService.enable2FA(userId);
            return ResponseEntity.ok("2FA Verified");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body("Invalid 2FA Code");
        }
    }


    @PostMapping("/user/2fa-status")
    public ResponseEntity<?> get2FAStatus() {
        User user = authUtil.loggedInUser();
        if (user != null) {
            return ResponseEntity.ok().body(Map.of("is2faEnabled", user.isTwoFactorEnabled()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found");
        }
    }


    @PostMapping("/public/verify-2fa-login")
    public ResponseEntity<?> verify2FALogin(@RequestParam int code,
                                            @RequestParam String jwtToken) {
        String username = jwtUtils.getUsernameFromToken(jwtToken);
        User user = userService.findByUsername(username);
        boolean isValid = userService.validate2FACode(user.getUserId(), code);
        if (isValid) {
            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getRoleName().name())
                    .toList();
            UserInfoResponse response =
                    new UserInfoResponse(
                            user.getUserId(),
                            user.getAccessToken(),
                            user.getUsername(),
                            roles
                    );
            return ResponseEntity.ok().body(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body("Invalid 2FA Code");
        }
    }

    @PostMapping("/validate/token")
    public ResponseEntity<?> validateToken(@RequestBody String token) {
        System.out.println("Validate token: " + token);
//        boolean valid = jwtUtils.validateToken(token);
        boolean valid = true;
        if (valid) {
            return ResponseEntity.ok().body(new MessageResponse("Token validated successfully!"));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Invalid token"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            userService.generatePasswordResetToken(email);
            return ResponseEntity.ok(new MessageResponse("Password reset email sent!"));
        } catch (Exception e) {
            System.out.println("Internal Server Email Error" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Internal Server Error"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }

}
