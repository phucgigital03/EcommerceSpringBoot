package com.example.eCommerceUdemy.security.jwt;

import com.example.eCommerceUdemy.exception.TokenExpireException;
import com.example.eCommerceUdemy.security.service.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private long jwtExpirationMs;

    @Value("${spring.app.jwtRefreshExpirationMs}")
    private long jwtRefreshExpirationMs;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtRefreshCookie;

    @Value("${spring.ecom.app.httpCookie}")
    private boolean httpCookie;

//    @Value("${spring.ecom.app.secureCookie}")
    private boolean secureCookie = true;

    public String extractJwtFromResponseCookie(String cookieHeader) {
        if (cookieHeader == null || !cookieHeader.startsWith(jwtRefreshCookie + "=")) {
            return null;
        }
        return cookieHeader.split(";")[0].split("=")[1]; // Extract JWT token
    }

    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtRefreshCookie);
        if (cookie != null) {
            return cookie.getValue();
        }else{
            return null;
        }
    }

    public ResponseCookie generateJwtRefreshCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateRefreshTokenFromUsername(userPrincipal.getUsername());
        ResponseCookie cookie = ResponseCookie.from(jwtRefreshCookie,jwt)
                .path("/api")
                .maxAge(24 * 60 * 60)
                .httpOnly(httpCookie)
                .secure(secureCookie)
                .sameSite("None")
                .build();
        return cookie;
    }

    public ResponseCookie getCleanJwtCookie() {
        ResponseCookie cookie = ResponseCookie.from(jwtRefreshCookie,"")
                .path("/api")  // Use same path as login
                .httpOnly(httpCookie)  // Ensure security
                .secure(secureCookie)  // Required for HTTPS on Render
                .sameSite("None")  // Required for cross-origin authentication
                .maxAge(0)  // Expire the cookie immediately
                .build();
        return cookie;
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization Header: {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String generateTokenFromUsername(UserDetailsImpl userDetailsImpl) {
        return Jwts.builder()
                .subject(userDetailsImpl.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .claim("nonce", UUID.randomUUID().toString())
                .claim("userId", userDetailsImpl.getId())
                .claim("is2faEnabled", userDetailsImpl.is2faEnabled())
                .signWith(key())
                .compact();
    }

    public String generateRefreshTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
                .claim("nonce", UUID.randomUUID().toString())
                .signWith(key())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key()).build()
                .parseSignedClaims(token)
                .getPayload().getSubject();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateToken(String authToken) {
        try{
            System.out.println("JwtUtils.java: Validation");
            Jwts.parser()
                    .verifyWith((SecretKey) key()).build()
                    .parseSignedClaims(authToken);
            return true;
        }catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        }catch (UnsupportedJwtException e){
            logger.error("JWT token is unsupported: {}", e.getMessage());
        }catch (IllegalArgumentException e){
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }catch (ExpiredJwtException e){
            logger.error("JWT token is expired: {}", e.getMessage());
            throw new TokenExpireException(HttpStatus.GONE,"Need to refresh token");
        }
        return false;
    }

}
