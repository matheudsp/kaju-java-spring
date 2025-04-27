package com.valedosol.kaju.configs.security;

import com.valedosol.kaju.service.CustomUserDetailsService;
import com.valedosol.kaju.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;



import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // System.out.println("\n--- New Request ---");
        // System.out.println("Request: " + method + " " + path);

        // Skip filter for auth endpoints
        if (path.startsWith("/auth/")) {
            // System.out.println("Skipping authentication for auth endpoint");
            filterChain.doFilter(request, response);
            return;
        }

        // Print all cookies for debugging
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            System.out.println("Cookies found in request:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie name: " + cookie.getName() + ", value: " + (cookie.getValue() != null ? cookie.getValue().substring(0, Math.min(10, cookie.getValue().length())) + "..." : "null"));
            }
        } else {
            System.out.println("No cookies found in request");
        }

        try {
            String jwt = jwtService.getJwtFromCookie(request);

            if (jwt == null || jwt.isEmpty()) {
                System.out.println("No JWT token found in cookies");
            } else {
                System.out.println("JWT token found: " + jwt.substring(0, 10) + "...");

                // Try to validate the token
                jwtService.validateToken(jwt);

                String userEmail = jwtService.extractEmail();
                

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);
                

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);
                
            }
        } catch (JwtException e) {
            // System.out.println("JWT validation failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // System.out.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}
