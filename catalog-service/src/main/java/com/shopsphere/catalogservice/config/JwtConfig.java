package com.shopsphere.catalogservice.config;

import com.shopsphere.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, accessTokenExpiry, refreshTokenExpiry);
    }
}