package com.shopsphere.authservice.security;

import com.shopsphere.authservice.service.AuthService;
import com.shopsphere.common.constants.AppConstants;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import com.shopsphere.common.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.shopsphere.authservice.controller.AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("Auth security header propagation tests")
class AuthSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Admin header access is allowed for protected user listing")
    void getAllUsers_withAdminHeaders_allowsAccess() throws Exception {
        when(authService.getAllUsers(eq(0), eq(10)))
                .thenReturn(ApiResponse.success(new PagedResponse<>(
                        java.util.List.of(),
                        0,
                        10,
                        0,
                        0,
                        true
                )));

        mockMvc.perform(get("/auth/users")
                        .header(AppConstants.HEADER_USER_ID, "1")
                        .header(AppConstants.HEADER_USER_ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(authService).getAllUsers(0, 10);
    }

    @Test
    @DisplayName("Missing forwarded auth headers is rejected for protected user listing")
    void getAllUsers_withoutHeaders_rejectsAccess() throws Exception {
        mockMvc.perform(get("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bearer token access is translated into forwarded headers")
    void getCurrentUser_withBearerToken_populatesForwardedHeaders()
            throws Exception {
        when(jwtUtil.validateToken("swagger-token")).thenReturn(true);
        when(jwtUtil.extractUserId("swagger-token")).thenReturn(7L);
        when(jwtUtil.extractRole("swagger-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractEmail("swagger-token"))
                .thenReturn("swagger.user@shopsphere.local");
        when(authService.getCurrentUser("swagger.user@shopsphere.local"))
                .thenReturn(ApiResponse.success(new UserDTO()));

        mockMvc.perform(get("/auth/me")
                        .header(AppConstants.AUTH_HEADER,
                                AppConstants.TOKEN_PREFIX + "swagger-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(authService).getCurrentUser("swagger.user@shopsphere.local");
    }
}
