package com.valedosol.kaju.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.token.secret}")
    private String secret;

    @Value("${jwt.token.expires}")
    private Long jwtExpiresMinutes;

    private Claims claims;

    public String generateToken(String email, HttpServletResponse response) {
        // System.out.println("Generating token for: " + email);

        String JWT = Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiresMinutes * 60 * 1000))
                .signWith(getSignInKey())
                .compact();

        // System.out.println("Token generated: " + JWT.substring(0, 10) + "...");

        Cookie cookie = new Cookie("JWT", JWT);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);

        // For older Servlet versions that don't support SameSite directly in Cookie
        response.setHeader("Set-Cookie", String.format("JWT=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
                JWT, 24 * 60 * 60));

        // System.out.println("Cookie added to response");

        return JWT;
    }

    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT".equals(cookie.getName())) { // Replace "jwt" with your actual cookie name
                    return cookie.getValue();
                }
            }
        }
        return null; // Return null if no JWT cookie found
    }

    public void validateToken(String token) throws JwtException {
        // System.out.println("Validating token: " + token.substring(0, 10) + "...");

        try {
            claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // System.out.println("Token validation successful");
            // Check if token is expired
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                throw new JwtException("Token expired");
            }
        } catch (JwtException e) {
            // System.out.println("Token validation failed: " + e.getMessage());
            throw new JwtException(e.getMessage());
        }
    }

    public void removeTokenFromCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("JWT", null);
        cookie.setPath("/");

        response.addCookie(cookie);
    }

    private SecretKey getSignInKey() {
        // SignatureAlgorithm.HS256, this.secret
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractEmail() {
        return claims.getSubject();
    }

}
