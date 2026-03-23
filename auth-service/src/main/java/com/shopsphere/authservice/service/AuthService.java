package com.shopsphere.authservice.service;

import com.shopsphere.authservice.dto.*;
import com.shopsphere.authservice.entity.RefreshToken;
import com.shopsphere.authservice.entity.User;
import com.shopsphere.authservice.repository.RefreshTokenRepository;
import com.shopsphere.authservice.repository.UserRepository;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ConflictException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.common.exception.UnauthorizedException;
import com.shopsphere.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ─── Signup ──────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<UserDTO> signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                "Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        return ApiResponse.success("User registered successfully", toDTO(saved));
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // Revoke existing tokens and issue new ones
        refreshTokenRepository.revokeAllUserTokens(user);

        String accessToken  = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Persist refresh token
        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(token);

        log.info("User logged in: {}", user.getEmail());

        return ApiResponse.success("Login successful",
                AuthResponse.of(accessToken, refreshToken,
                        user.getEmail(), user.getRole().name()));
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<AuthResponse> refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = stored.getUser();

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        log.info("Token refreshed for user: {}", user.getEmail());

        return ApiResponse.success("Token refreshed",
                AuthResponse.of(newAccessToken, refreshToken,
                        user.getEmail(), user.getRole().name()));
    }

    // ─── Get Current User ────────────────────────────────────────────────────

    public ApiResponse<UserDTO> getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
        return ApiResponse.success(toDTO(user));
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Void> logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("User logged out: {}", email);
        return ApiResponse.success("Logged out successfully", null);
    }

    // ─── Change Password ─────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Void> changePassword(String email,
                                             ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all tokens — force re-login after password change
        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("Password changed for user: {}", email);
        return ApiResponse.success("Password changed successfully", null);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
    
    public ApiResponse<PagedResponse<UserDTO>> getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        List<UserDTO> content = users.getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ApiResponse.success(new PagedResponse<>(
                content,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isLast()));
    }
}