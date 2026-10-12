package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.authUtils.JwtInterceptor;

/**
 * *JWT interceptor:
 * 1. Extract JWT token from Authorization header
 * 2. Validate JWT signature
 * 3. Extract clientId and role from token payload
 * 4. Set SecurityContext with role-based authorities
 * 5. Make clientId available to services via ClientIdContextHolder
 * 
 * Protected endpoints: All /api/v1/** routes require valid JWT with appropriate role
 * - CLIENT role: Access /clients endpoints
 * - EMPLOYEE_ADMIN role: Access /admin endpoints
 * - EMPLOYEE_REPORTER role: Access /reporter endpoints
 */
@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class AuthConfig implements WebMvcConfigurer {
    
    private final JwtInterceptor jwtInterceptor;
    
    public AuthConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**");  // Apply to ALL API endpoints - no exclusions (auth handled by NestJS)
    }
}
