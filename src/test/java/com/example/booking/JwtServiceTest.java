package com.example.booking;

import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.security.AppUserDetails;
import com.example.booking.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-123456";

    private AppUserDetails user() {
        return new AppUserDetails(new User("alice", "hash", Role.USER));
    }

    @Test
    void generatedTokenRoundTrips() {
        JwtService svc = new JwtService(SECRET, 60_000);
        assertEquals("alice", svc.extractUsername(svc.generateToken(user())));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService svc = new JwtService(SECRET, -1_000);
        String token = svc.generateToken(user());
        assertThrows(ExpiredJwtException.class, () -> svc.extractUsername(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtService("another-secret-another-secret-1234567890", 60_000).generateToken(user());
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60_000).extractUsername(token));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 1000));
    }
}
