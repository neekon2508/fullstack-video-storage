package com.api.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import backend.auth.service.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Getter
public class JwtUtil {

    @Value("${jwt.access-token-secret-key}")
    private String accessSecret;

    @Value("${jwt.refresh-token-secret-key}")
    private String refreshSecret;

    @Value("${jwt.access-token-expiration-time}")
    private long accessExp;

    @Value("${jwt.refresh-token-expiration-time}")
    private long refreshExp;

    public String generateAccessToken(UserDetailsImpl user) {
        LocalDateTime expiry = LocalDateTime.now().plus(Duration.ofSeconds(accessExp));
        Date expiryDate = Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("id", user.getId())
                .claim("username", user.getUsername())
                .claim("tenant_id", user.getTenantId())
                .claim("role", user.getRole())
                .claim("functions", user.getFunctions())
                .signWith(Keys.hmacShaKeyFor(accessSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        LocalDateTime expiry = LocalDateTime.now().plus(Duration.ofSeconds(refreshExp));
        Date expiryDate = Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(Keys.hmacShaKeyFor(refreshSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, boolean isRefresh) {

        String secret = isRefresh ? refreshSecret : accessSecret;
        Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token);
        return true;
    }

    public Claims extractClaims(String token, boolean isRefresh) {
        String secret = isRefresh ? refreshSecret : accessSecret;
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // public List<String> extractRoles(String token) {
    // List<?> roles = extractClaims(token, false).get("roles", List.class);

    // if (roles == null) return Collections.emptyList();

    // return roles.stream().map(Object::toString).toList();
    // }

    public long getReaminingTime(String token, boolean isRefresh) {
        try {
            String secret = isRefresh ? refreshSecret : accessSecret;
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Math.max(0, (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000);
        } catch (Exception e) {
            return 0;
        }

    }
}
