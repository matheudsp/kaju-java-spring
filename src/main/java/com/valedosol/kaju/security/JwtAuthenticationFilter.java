package com.valedosol.kaju.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

import com.valedosol.kaju.feature.auth.service.CustomUserDetailsService;
import com.valedosol.kaju.feature.auth.service.JwtService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/auth/",
            "/api/v2/stripe/",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs"
    );

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // Skip authentication for public paths
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateRequest(request);
        } catch (JwtException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private void authenticateRequest(HttpServletRequest request) {
        String jwt = jwtService.getJwtFromCookie(request);
            
        // If no JWT token found, authentication will be handled by security config
        if (jwt == null) {
            return;
        }

        // Validate the token and set authentication
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
}