package com.elmeftouhi.expense.security;

import com.elmeftouhi.expense.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(JwtProperties jwtProperties) {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMinutes = jwtProperties.getExpirationMinutes();
    }

    public String generateToken(AppUser user, Long selectedTenantId, Set<Long> allowedTenantIds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationMinutes * 60);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("tenantId", selectedTenantId)
                .claim("tenants", allowedTenantIds)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public JwtPrincipal parse(String token) {
        Claims claims = parseClaims(token);
        Long userId = claims.get("uid", Long.class);
        Long selectedTenantId = claims.get("tenantId", Long.class);
        String username = claims.getSubject();
        Collection<?> rawTenants = claims.get("tenants", Collection.class);

        Set<Long> allowedTenantIds = new HashSet<>();
        if (rawTenants != null) {
            for (Object rawTenantId : rawTenants) {
                if (rawTenantId instanceof Number n) {
                    allowedTenantIds.add(n.longValue());
                }
            }
        }

        if (selectedTenantId == null || !allowedTenantIds.contains(selectedTenantId)) {
            throw new IllegalArgumentException("Invalid tenant claim in JWT");
        }

        return new JwtPrincipal(userId, username, selectedTenantId, allowedTenantIds);
    }

    public Instant extractExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        if (expiration == null) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
        return expiration.toInstant();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }
}

