package com.shopsphere.apigateway.filter;

import com.shopsphere.common.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Filter Unit Tests")
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter; // Removed reflection setup

    @Test
    @DisplayName("getOrder returns negative one for high priority")
    void getOrder_returnsNegativeOne() {
        assertThat(authenticationFilter.getOrder()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Public path passes through without authentication")
    void filter_publicPath_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
<<<<<<< HEAD
=======
    @DisplayName("Public catalog product detail path passes through without authentication")
    void filter_publicCatalogProductDetail_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/catalog/products/42").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
>>>>>>> 0a1129c (Complete Project)
    @DisplayName("Missing Authorization header returns 401")
    void filter_missingAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/orders/create").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Invalid token returns 401")
    void filter_invalidToken_returns401() {
        String token = "invalid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/orders/create")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(false);

        StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("Valid token extracts claims and adds headers to request")
    void filter_validToken_addsUserHeadersAndContinues() {
        // Arrange
        String token = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/orders/create")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractUserId(token)).thenReturn(1L);
        when(jwtUtil.extractRole(token)).thenReturn("CUSTOMER");
        when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                .verifyComplete();

        // Assert
        // We use ArgumentCaptor because the filter passes a MUTATED exchange, not the original one
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(filterChain).filter(exchangeCaptor.capture());

        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        HttpHeaders headers = capturedExchange.getRequest().getHeaders();

        assertThat(headers.getFirst("X-User-Id")).isEqualTo("1");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("CUSTOMER");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("JwtUtil throws exception returns 401")
    void filter_jwtUtilThrowsException_returns401() {
        String token = "malformed.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/orders/create")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("JWT parsing error"));

        StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 0a1129c (Complete Project)
