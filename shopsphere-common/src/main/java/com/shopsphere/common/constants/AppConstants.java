package com.shopsphere.common.constants;

public class AppConstants {

    private AppConstants() {
        // utility class — prevent instantiation
    }

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final int MAX_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // Stock
    public static final int LOW_STOCK_THRESHOLD = 10;

    // Gateway headers forwarded to downstream services
    public static final String HEADER_USER_ID   = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    // JWT
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTH_HEADER  = "Authorization";

    // Roles
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ADMIN    = "ADMIN";
}
