package com.example.eCommerceUdemy.security.jwt;


import com.example.eCommerceUdemy.exception.APIException;
import com.example.eCommerceUdemy.exception.TokenExpireException;
import com.example.eCommerceUdemy.model.User;
import com.example.eCommerceUdemy.payload.APIResponse;
import com.example.eCommerceUdemy.repository.UserRepository;
import com.example.eCommerceUdemy.security.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    UserDetailsServiceImpl userDetailsService;
    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain
    ) throws ServletException, IOException {
//      For public endpoints
        String requestURI = request.getRequestURI();
        logger.debug("AuthTokenFilter called for URI: {}", requestURI);
        if(isPublicEndpoint(requestURI)){
            logger.error("Skipping authentication for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

//      For private endpoints
        try{
            String jwt = parseJwt(request);
            if(jwt != null) {
                User user = userRepository.findByAccessToken(jwt)
                        .orElse(null);
                if(user != null) {
                    String jwtDB = user.getAccessToken();
                    logger.debug("JWT Token from DB: {}", jwtDB);
                    if(jwtDB != null && !user.getIsTokenRevoked()) {

                        if(jwtUtils.validateToken(jwtDB)){
                            String username = jwtUtils.getUsernameFromToken(jwt);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            logger.debug("Roles from JWT: {}", userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }else{
                        logger.debug("JWT token DB is revoked or expired");
                        setUnauthorizedResponse(response, "JWT token DB is revoked or expired");
                        return;
                    }
                }else{
                    logger.debug("User DB is null or accessToken is revoked");
                    setUnauthorizedResponse(response, "User DB is null or accessToken is revoked");
                    return;
                }
            }else{
                logger.debug("JWT token is missing");
                setUnauthorizedResponse(response, "JWT token is missing");
                return;
            }
            filterChain.doFilter(request, response);
        } catch (TokenExpireException e){
            logger.error("Handle token expire: " + e.getMessage());
            setUnauthorizedResponse(response, e.getMessage());
        } catch (Exception e) {
            logger.error("Cannot set user authentication" + e.getMessage());
            setUnauthorizedResponse(response, "Authentication error: " + e.getMessage());
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        String jwtCookie = jwtUtils.getJwtFromCookie(request);
        logger.debug("AuthTokenFilter.java: {}", jwt);
        logger.debug("AuthTokenFilter.java jwtCookie: {}", jwtCookie);
        return jwt;
    }

    private boolean isPublicEndpoint(String uri) {
        if(
                uri.startsWith("/api/auth/enable-2fa") ||
                uri.startsWith("/api/auth/disable-2fa") ||
                uri.startsWith("/api/auth/verify-2fa") ||
                uri.startsWith("/api/auth/user/2fa-status") ||
                uri.startsWith("/api/auth/user")
        ){
            return false;
        }
        return uri.startsWith("/api/auth/") ||  // Authentication routes
                uri.startsWith("/v3/api-docs/") || // API Docs
                uri.startsWith("/swagger-ui/") || // Swagger UI
                uri.startsWith("/images/") || // Public images
                uri.startsWith("/api/public/") || // Public API
                uri.startsWith("/api/payment/vn-pay/"); // Public Payment VNPAY
    }

    private void setUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        if(message.equals("Need to refresh token")){
            response.setStatus(HttpServletResponse.SC_GONE);
        }else{
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        APIResponse apiResponse = new APIResponse(message, false);
        new ObjectMapper().writeValue(response.getOutputStream(), apiResponse);
    }

}
