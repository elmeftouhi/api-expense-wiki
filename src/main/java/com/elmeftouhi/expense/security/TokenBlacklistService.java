package com.elmeftouhi.expense.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final ConcurrentMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiresAt) {
        revokedTokens.put(token, expiresAt);
    }

    public boolean isRevoked(String token) {
        Instant expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }

        if (expiresAt.isBefore(Instant.now())) {
            revokedTokens.remove(token, expiresAt);
            return false;
        }

        return true;
    }
}

