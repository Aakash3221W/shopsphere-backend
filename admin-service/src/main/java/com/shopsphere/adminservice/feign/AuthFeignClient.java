package com.shopsphere.adminservice.feign;

import com.shopsphere.adminservice.feign.fallback.AuthFeignClientFallbackFactory;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "auth-service",
        fallbackFactory = AuthFeignClientFallbackFactory.class
)
public interface AuthFeignClient {

    @GetMapping("/auth/users")
    ApiResponse<PagedResponse<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);
}