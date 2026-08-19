package com.api.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.api.auth.service.UserDetailsImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
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

    @Value("${request.cookie.refresh-cookie-name}")
    private String refreshCookieName;

    public String generateAccessToken(Long id, String username) {
        LocalDateTime expiry = LocalDateTime.now().plus(Duration.ofSeconds(accessExp));
        Date expiryDate = Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("id", id)
                .claim("username", username)
                .signWith(Keys.hmacShaKeyFor(accessSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Long id, String username) {
        LocalDateTime expiry = LocalDateTime.now().plus(Duration.ofSeconds(refreshExp));
        Date expiryDate = Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setId(Long.toString(id))
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

    public Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(refreshCookieName, refreshToken);
        cookie.setMaxAge((int) refreshExp);
        cookie.setSecure(false);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/movie");
        return cookie;
    }
}
