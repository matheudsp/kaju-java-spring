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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // Skip authentication check for these paths
        if (path.startsWith("/auth/") || 
            path.startsWith("/api/v2/stripe/") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/api-docs") ||
            path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = jwtService.getJwtFromCookie(request);
            
            // If no JWT token found, just continue the filter chain (security config will handle this)
            if (jwt == null) {
                filterChain.doFilter(request, response);
                return;
            }

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

        } catch (JwtException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}