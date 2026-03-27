package com.shopsphere.apigateway.filter;

import com.shopsphere.common.constants.AppConstants;
import com.shopsphere.common.util.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    private static final List<Predicate<String>> PUBLIC_PATHS = List.of(
            path -> path.startsWith("/gateway/auth/signup"),
            path -> path.startsWith("/gateway/auth/login"),
            path -> path.startsWith("/gateway/auth/refresh"),
            path -> path.startsWith("/gateway/catalog/products"),
            path -> path.startsWith("/gateway/catalog/categories"),
            path -> path.startsWith("/gateway/catalog/featured"),
            path -> path.startsWith("/actuator/health")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String userId = String.valueOf(jwtUtil.extractUserId(token));
            String role   = jwtUtil.extractRole(token);
            String email  = jwtUtil.extractEmail(token);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set(AppConstants.HEADER_USER_ID, userId);
                        headers.set(AppConstants.HEADER_USER_ROLE, role);
                        headers.set(AppConstants.HEADER_USER_EMAIL, email);
                    }))
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(matcher -> matcher.test(path));
    }
}
