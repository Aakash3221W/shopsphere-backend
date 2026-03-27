package com.shopsphere.authservice.security;

import com.shopsphere.common.constants.AppConstants;
import com.shopsphere.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            HeaderAuthFilter headerAuthFilter)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs"
                ).permitAll()
                // all requests permitted at service level
                // gateway handles JWT validation and role checks
                .anyRequest().permitAll()
            )
            .addFilterBefore(headerAuthFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

@Component
@RequiredArgsConstructor
class HeaderAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(AppConstants.HEADER_USER_ID);
        String role = request.getHeader(AppConstants.HEADER_USER_ROLE);
        String email = request.getHeader(AppConstants.HEADER_USER_EMAIL);

        if (userId == null || role == null || email == null) {
            String authHeader = request.getHeader(AppConstants.AUTH_HEADER);
            if (authHeader != null
                    && authHeader.startsWith(AppConstants.TOKEN_PREFIX)) {
                String token = authHeader.substring(
                        AppConstants.TOKEN_PREFIX.length());
                if (jwtUtil.validateToken(token)) {
                    if (userId == null) {
                        Long tokenUserId = jwtUtil.extractUserId(token);
                        userId = tokenUserId != null ? tokenUserId.toString()
                                : null;
                    }
                    if (role == null) {
                        role = jwtUtil.extractRole(token);
                    }
                    if (email == null) {
                        email = jwtUtil.extractEmail(token);
                    }
                }
            }
        }

        HttpServletRequest requestToUse = request;
        Map<String, String> derivedHeaders = new LinkedHashMap<>();
        if (userId != null && request.getHeader(AppConstants.HEADER_USER_ID) == null) {
            derivedHeaders.put(AppConstants.HEADER_USER_ID, userId);
        }
        if (role != null && request.getHeader(AppConstants.HEADER_USER_ROLE) == null) {
            derivedHeaders.put(AppConstants.HEADER_USER_ROLE, role);
        }
        if (email != null
                && request.getHeader(AppConstants.HEADER_USER_EMAIL) == null) {
            derivedHeaders.put(AppConstants.HEADER_USER_EMAIL, email);
        }

        if (!derivedHeaders.isEmpty()) {
            requestToUse = new HeaderAugmentingRequestWrapper(request,
                    derivedHeaders);
        }

        if (userId != null && role != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(requestToUse));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(requestToUse, response);
    }
}

class HeaderAugmentingRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> headers;

    HeaderAugmentingRequestWrapper(HttpServletRequest request,
            Map<String, String> headers) {
        super(request);
        this.headers = headers;
    }

    @Override
    public String getHeader(String name) {
        String headerValue = headers.get(name);
        return headerValue != null ? headerValue : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String headerValue = headers.get(name);
        if (headerValue != null) {
            return Collections.enumeration(List.of(headerValue));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headerNames = Collections.list(super.getHeaderNames());
        for (String headerName : headers.keySet()) {
            if (!headerNames.contains(headerName)) {
                headerNames.add(headerName);
            }
        }
        return Collections.enumeration(headerNames);
    }
}
