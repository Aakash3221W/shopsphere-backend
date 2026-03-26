package com.shopsphere.adminservice.feign.fallback;

import com.shopsphere.adminservice.feign.AuthFeignClient;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import com.shopsphere.common.exception.ServiceUnavailableException; // Import your custom exception
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthFeignClientFallbackFactory
        implements FallbackFactory<AuthFeignClient> {

    @Override
    public AuthFeignClient create(Throwable cause) {
        return new AuthFeignClient() {

            @Override
            public ApiResponse<PagedResponse<UserDTO>> getAllUsers(
                    int page, int size) {
                // Log the actual cause so you can debug if it was a timeout or connection refused
                log.error("Auth Service unavailable - Reason: {}", cause.getMessage());
                
                // THROW THE CUSTOM EXCEPTION
                throw new ServiceUnavailableException(
                        "Auth Service is currently unavailable. Please try again later.");
            }
        };
    }
}