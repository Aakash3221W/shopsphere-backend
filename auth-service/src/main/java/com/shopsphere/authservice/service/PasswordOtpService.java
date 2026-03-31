package com.shopsphere.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PasswordOtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final String OTP_KEY_PREFIX = "auth:password-reset:";

    private final StringRedisTemplate stringRedisTemplate;

    public String generateAndStoreOtp(String email) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        stringRedisTemplate.opsForValue().set(buildKey(email), otp, OTP_TTL);
        return otp;
    }

    public boolean matches(String email, String otp) {
        String storedOtp = stringRedisTemplate.opsForValue().get(buildKey(email));
        return storedOtp != null && storedOtp.equals(otp);
    }

    public void clear(String email) {
        stringRedisTemplate.delete(buildKey(email));
    }

    private String buildKey(String email) {
        return OTP_KEY_PREFIX + email.toLowerCase();
    }
}
