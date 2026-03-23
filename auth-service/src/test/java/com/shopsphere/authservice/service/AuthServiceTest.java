package com.shopsphere.authservice.service;

import com.shopsphere.authservice.dto.*;
import com.shopsphere.authservice.entity.RefreshToken;
import com.shopsphere.authservice.entity.User;
import com.shopsphere.authservice.repository.RefreshTokenRepository;
import com.shopsphere.authservice.repository.UserRepository;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.UserDTO;
import com.shopsphere.common.enums.Role;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ConflictException;
import com.shopsphere.common.exception.UnauthorizedException;
import com.shopsphere.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@shopsphere.com")
                .password("encodedPassword")
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();

        signupRequest = new SignupRequest();
        signupRequest.setName("Test User");
        signupRequest.setEmail("test@shopsphere.com");
        signupRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@shopsphere.com");
        loginRequest.setPassword("password123");
    }

    // ─── Signup Tests ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Signup — success registers new user")
    void signup_success() {
        when(userRepository.existsByEmail(signupRequest.getEmail()))
                .thenReturn(false);
        when(passwordEncoder.encode(signupRequest.getPassword()))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        ApiResponse<UserDTO> response = authService.signup(signupRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getEmail())
                .isEqualTo("test@shopsphere.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Signup — throws ConflictException when email already registered")
    void signup_emailAlreadyExists_throwsConflict() {
        when(userRepository.existsByEmail(signupRequest.getEmail()))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    // ─── Login Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login — success returns access and refresh tokens")
    void login_success() {
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(),
                testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any(), any()))
                .thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(any()))
                .thenReturn("refreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(new RefreshToken());

        ApiResponse<AuthResponse> response = authService.login(loginRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getData().getRefreshToken()).isEqualTo("refreshToken");
        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
    }

    @Test
    @DisplayName("Login — throws UnauthorizedException for unknown email")
    void login_userNotFound_throwsUnauthorized() {
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Login — throws UnauthorizedException for wrong password")
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(),
                testUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Login — throws UnauthorizedException for disabled account")
    void login_disabledAccount_throwsUnauthorized() {
        testUser.setActive(false);
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(),
                testUser.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("disabled");
    }

    // ─── Logout Tests ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Logout — success revokes all user tokens")
    void logout_success() {
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));

        ApiResponse<Void> response = authService.logout(testUser.getEmail());

        assertThat(response.isSuccess()).isTrue();
        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
    }

    // ─── Change Password Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Change Password — success")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("password123");
        request.setNewPassword("newPassword123");

        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getOldPassword(),
                testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword()))
                .thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        ApiResponse<Void> response = authService.changePassword(
                testUser.getEmail(), request);

        assertThat(response.isSuccess()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
    }

    @Test
    @DisplayName("Change Password — throws BadRequestException for wrong old password")
    void changePassword_wrongOldPassword_throwsBadRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getOldPassword(),
                testUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(
                testUser.getEmail(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Old password is incorrect");
    }

    // ─── Refresh Token Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh — throws UnauthorizedException for revoked token")
    void refresh_revokedToken_throwsUnauthorized() {
        RefreshToken revokedToken = RefreshToken.builder()
                .token("revokedToken")
                .isRevoked(true)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .user(testUser)
                .build();

        when(refreshTokenRepository.findByToken("revokedToken"))
                .thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh("revokedToken"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("Refresh — throws UnauthorizedException for expired token")
    void refresh_expiredToken_throwsUnauthorized() {
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expiredToken")
                .isRevoked(false)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .user(testUser)
                .build();

        when(refreshTokenRepository.findByToken("expiredToken"))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh("expiredToken"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }
}