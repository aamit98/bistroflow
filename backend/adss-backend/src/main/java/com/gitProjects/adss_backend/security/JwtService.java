package com.gitProjects.adss_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final String secret;

    private final long expirationMinutes;

    private Key signingKey;

    public JwtService(
            @Value("${adss.jwt.secret}") String secret,
            @Value("${adss.jwt.expiration-minutes:60}") long expirationMinutes
    ) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    @PostConstruct
    void initializeSigningKey() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Missing adss.jwt.secret configuration. Set ADSS_JWT_SECRET to a Base64-encoded 256-bit key.");
        }

        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("adss.jwt.secret must be Base64 encoded", ex);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits when decoded.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getSigningKey() {
        if (signingKey == null) {
            throw new IllegalStateException("Signing key not initialized");
        }
        return signingKey;
    }

    public String generateToken(
            String subject,
            Map<String, Object> extraClaims
    ) {
        Instant now = Instant.now();
        Instant exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(subject)
                .addClaims(extraClaims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
