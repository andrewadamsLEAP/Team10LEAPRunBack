package com.example.authUtils;

import com.example.exceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Interceptor that extracts and validates clientId from JWT bearer tokens.
 * 
 * Flow:
 * 1. Extract JWT token from Authorization header
 * 2. Validate JWT signature
 * 3. Extract clientId from token payload
 * 4. Store clientId in ClientIdContextHolder for access in services
 * 5. After request completes, clear the context
 * 
 * The JWT is expected in the format: Authorization: Bearer <token>
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtil jwtUtil;
    
    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.warn("Missing or invalid Authorization header");
                throw new UnauthorizedException("Missing or invalid Authorization header");
            }
            
            // Extract token from "Bearer <token>"
            String token = authHeader.substring(BEARER_PREFIX.length());
            
            // Validate JWT signature
            if (!jwtUtil.validateToken(token)) {
                logger.warn("JWT token validation failed");
                throw new UnauthorizedException("Invalid or expired token");
            }
            
            // Extract clientId from token payload
            Long clientId = jwtUtil.getClientIdFromToken(token);
            
            if (clientId == null) {
                logger.warn("Token does not contain clientId");
                throw new UnauthorizedException("Token does not contain clientId");
            }
            
            // Store clientId in thread-local context for access in services
            ClientIdContextHolder.setClientId(clientId);
            
            // Extract role and set up Spring Security context with role authorities
            String role = jwtUtil.getRoleFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            
            if (role != null) {
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.info("Request authenticated with clientId: {}, role: {}", clientId, role);
            } else {
                logger.warn("Token does not contain role claim");
            }
            
            return true;
            
        } catch (UnauthorizedException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            return false;
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Always clean up the context after request processing
        ClientIdContextHolder.clear();
        logger.debug("Cleared clientId context after request");
    }
}
