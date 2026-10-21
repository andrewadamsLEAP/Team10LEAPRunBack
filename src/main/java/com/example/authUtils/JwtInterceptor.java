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
 * Interceptor that extracts and validates identity and role from JWT bearer tokens.
 * 
 * Flow:
 * 1. Extract JWT token from Authorization header
 * 2. Validate JWT signature
 * 3. Extract the user ID and role from the token payload
 * 4. Store clientId in ClientIdContextHolder for client requests
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

        // Browser CORS preflight requests do not carry the application JWT.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.warn("Missing or invalid Authorization header");
                throw new UnauthorizedException("Missing or invalid Authorization header");
            }
            
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                throw new UnauthorizedException("Missing or invalid Authorization header");
            }
            
            // Validate JWT signature
            if (!jwtUtil.validateToken(token)) {
                logger.warn("JWT token validation failed");
                throw new UnauthorizedException("Invalid or expired token");
            }
            
            Long userId = jwtUtil.getUserIdFromToken(token);

            if (userId == null || userId <= 0) {
                throw new UnauthorizedException("Token does not contain a valid subject ID");
            }
            
            String role = jwtUtil.getRoleFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String actorType = jwtUtil.getActorTypeFromToken(token);
            String tokenUse = jwtUtil.getTokenUseFromToken(token);

            if (actorType == null
                    || (!"client".equalsIgnoreCase(actorType)
                    && !"employee".equalsIgnoreCase(actorType))) {
                throw new UnauthorizedException("Token does not contain a valid actorType claim");
            }

            if (!"access".equalsIgnoreCase(tokenUse)) {
                throw new UnauthorizedException("Token is not an access token");
            }

            String authority;
            if ("client".equalsIgnoreCase(actorType)) {
                ClientIdContextHolder.setClientId(userId);
                authority = "CLIENT";
            } else {
                if (role == null || role.isBlank()) {
                    throw new UnauthorizedException("Employee token does not contain a role claim");
                }
                authority = normalizeEmployeeRole(role);
            }

            AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                    userId,
                    username,
                    actorType,
                    role,
                    jwtUtil.getTokenIdFromToken(token),
                    jwtUtil.getSessionIdFromToken(token));
            AuthContextHolder.set(authenticatedUser);

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + authority));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("Request authenticated with userId: {}, actorType: {}, authority: {}",
                    userId, actorType, authority);
            
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
        AuthContextHolder.clear();
        SecurityContextHolder.clearContext();
        logger.debug("Cleared clientId context after request");
    }

    private String normalizeEmployeeRole(String role) throws UnauthorizedException {
        return switch (role.toLowerCase()) {
            case "admin", "employee_admin" -> "EMPLOYEE_ADMIN";
            case "reporter", "employee_reporter" -> "EMPLOYEE_REPORTER";
            default -> throw new UnauthorizedException("Unsupported employee role");
        };
    }
}
