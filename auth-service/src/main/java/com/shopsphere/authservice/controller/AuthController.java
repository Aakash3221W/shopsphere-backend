package com.shopsphere.authservice.controller;

import com.shopsphere.authservice.dto.*;
import com.shopsphere.authservice.service.AuthService;
import com.shopsphere.common.constants.AppConstants;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /auth/signup
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserDTO>> signup(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.signup(request));
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    // GET /auth/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(
            @RequestHeader(AppConstants.HEADER_USER_ID) String userId,
            @RequestHeader(AppConstants.HEADER_USER_ROLE) String role,
            @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(authService.getCurrentUser(email));
    }

    // POST /auth/logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(authService.logout(email));
    }

    // PUT /auth/changepassword
    @PutMapping("/changepassword")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(email, request));
    }
    
    // GET /auth/users — Admin only, called by Admin Service via Feign
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(authService.getAllUsers(page, size));
    }
}