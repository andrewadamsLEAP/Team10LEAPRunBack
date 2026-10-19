package com.example.authUtils;

import com.example.exceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
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

        // Only enforce JWT on endpoints explicitly marked as secured.
        if (!isJwtRequired(handler)) {
            logger.debug("Skipping JWT auth for public endpoint: {} {}", request.getMethod(), request.getRequestURI());
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
            
            // NestJS stores the authenticated record ID in sub. It is a clientId
            // only for client tokens; employee tokens do not populate this context.
            Long clientId = jwtUtil.getClientIdFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);

            if (userId == null || userId <= 0) {
                throw new UnauthorizedException("Token does not contain a valid subject ID");
            }
            
            // Extract role and set up Spring Security context with role authorities
            String role = jwtUtil.getRoleFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String actorType = jwtUtil.getActorTypeFromToken(token);

            if (actorType == null
                    || (!"client".equalsIgnoreCase(actorType)
                    && !"employee".equalsIgnoreCase(actorType))) {
                throw new UnauthorizedException("Token does not contain a valid actorType claim");
            }

            // Only client-scoped operations need a client ID. Employee tokens do not have one.
            if (clientId != null) {
                ClientIdContextHolder.setClientId(clientId);
            }
            
            if (role != null) {
                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.info("Request authenticated with userId: {}, clientId: {}, role: {}",
                    userId, clientId, role);
            } else {
                logger.warn("JWT rejected for {} {}: token does not contain role claim",
                        request.getMethod(), request.getRequestURI());
                throw new UnauthorizedException("Token does not contain role claim");
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
        SecurityContextHolder.clearContext();
        logger.debug("Cleared clientId context after request");
    }

    private boolean isJwtRequired(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }

        PreAuthorize methodAnnotation = handlerMethod.getMethodAnnotation(PreAuthorize.class);
        if (methodAnnotation != null) {
            return !isPermitAllExpression(methodAnnotation.value());
        }

        PreAuthorize classAnnotation = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
        if (classAnnotation != null) {
            return !isPermitAllExpression(classAnnotation.value());
        }

        // No method/class security annotation: treat as public route.
        return false;
    }

    private boolean isPermitAllExpression(String expression) {
        if (expression == null) {
            return false;
        }
        return "permitAll()".equals(expression.replace(" ", ""));
    }
}
