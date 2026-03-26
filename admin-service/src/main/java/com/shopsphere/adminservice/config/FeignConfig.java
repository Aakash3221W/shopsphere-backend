package com.shopsphere.adminservice.config;

import com.shopsphere.common.constants.AppConstants;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
            if (attributes != null) {
                String userId = attributes.getRequest()
                        .getHeader(AppConstants.HEADER_USER_ID);
                String role = attributes.getRequest()
                        .getHeader(AppConstants.HEADER_USER_ROLE);
                String email = attributes.getRequest()
                        .getHeader("X-User-Email");
                if (userId != null) {
                    requestTemplate.header(AppConstants.HEADER_USER_ID, userId);
                }
                if (role != null) {
                    requestTemplate.header(AppConstants.HEADER_USER_ROLE, role);
                }
                if (email != null) {
                    requestTemplate.header("X-User-Email", email);
                }
            }
        };
    }
}