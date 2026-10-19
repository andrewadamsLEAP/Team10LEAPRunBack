package com.example.authUtils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Utility class for validating and extracting claims from JWT tokens.
 * 
 * This utility creates development tokens and validates tokens issued by the
 * authentication service using the shared signing secret.
 * 
 * The JWT token contains (set by NestJS):
 * - sub (client or employee ID)
 * - username
 * - actorType (client or employee)
 * - role (for employee tokens)
 * - issuedAt
 * - expiresAt
 */
@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${auth.local-token.enabled:false}")
    private boolean localTokenEnabled;

    // Kept for source compatibility with older callers; login paths use the identity-aware overload.
    @Deprecated
    public String generateToken() {
        return generateToken(0L, "development", "client", "CLIENT");
    }

    public String generateToken(Long userId, String username, String actorType, String role) {
        if (!localTokenEnabled) {
            throw new IllegalStateException("NestJS is the configured token issuer");
        }

        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtExpirationMs);
        SecretKey key = signingKey();

        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("actorType", actorType)
                .claim("token_use", "access")
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256);

        if (role != null && !role.isBlank()) {
            builder.claim("role", role);
        }

        return builder.compact();
    }
    
    /**
     * Validate JWT token signature and expiration
     * 
     * @param token the JWT token
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);

            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                logger.warn("JWT token has expired");
                return false;
            }

            String tokenUse = claims.get("token_use", String.class);
            if (!"access".equalsIgnoreCase(tokenUse)) {
                logger.warn("JWT rejected because token_use is not access: {}", tokenUse);
                return false;
            }

            if (claims.getSubject() == null || getUsername(claims) == null) {
                logger.warn("JWT rejected because sub or username is missing");
                return false;
            }

            String actorType = claims.get("actorType", String.class);
            if (!"client".equalsIgnoreCase(actorType)
                    && !"employee".equalsIgnoreCase(actorType)) {
                logger.warn("JWT rejected because actorType is invalid: {}", actorType);
                return false;
            }

            if ("employee".equalsIgnoreCase(actorType) && getRoleFromClaims(claims) == null) {
                logger.warn("JWT rejected because employee role is missing or invalid");
                return false;
            }

            logger.debug("JWT token validated successfully");
            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token: {}", e.getMessage());
        } catch (JwtException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Extract username from JWT token
     * 
     * @param token the JWT token
     * @return the username (subject)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return getUsername(claims);
    }
    
    /**
     * Extract clientId from JWT token
     * This is the key method for getting clientId from the token
     * 
     * @param token the JWT token
     * @return the clientId
     */
    public Long getClientIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Long clientId = getLongClaim(claims, "clientId");
        if (clientId != null) {
            return clientId;
        }

        if ("client".equalsIgnoreCase(claims.get("actorType", String.class))) {
            return parseLong(claims.getSubject());
        }

        return null;
    }

    /**
     * Extract the authenticated database record ID from the NestJS subject claim.
     * For clients this is the clientId; for employees this is the employeeId.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Long userId = getLongClaim(claims, "userId");
        return userId != null ? userId : parseLong(claims.getSubject());
    }

    public String getActorTypeFromToken(String token) {
        return getClaimsFromToken(token).get("actorType", String.class);
    }

    /**
     * Extract role from JWT token
     * Role is set by NestJS during token generation
     * 
     * @param token the JWT token
     * @return the role (CLIENT, EMPLOYEE_ADMIN, or EMPLOYEE_REPORTER)
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return getRoleFromClaims(claims);
    }

    private String getRoleFromClaims(Claims claims) {
        String actorType = claims.get("actorType", String.class);
        String role = claims.get("role", String.class);

        if ("client".equalsIgnoreCase(actorType)) {
            return "CLIENT";
        }

        if (role == null) {
            return null;
        }

        return switch (role.toUpperCase()) {
            case "ADMIN", "MANAGER", "EMPLOYEE_ADMIN" -> "EMPLOYEE_ADMIN";
            case "REPORTER", "EMPLOYEE_REPORTER" -> "EMPLOYEE_REPORTER";
            default -> null;
        };
    }
    
    /**
     * Extract all claims from JWT token
     * 
     * @param token the JWT token
     * @return the Claims object
     */
    private Claims getClaimsFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is empty");
        }

        SecretKey key = signingKey();
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey signingKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Long getLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return parseLong(value == null ? null : value.toString());
    }

    private String getUsername(Claims claims) {
        String username = claims.get("username", String.class);
        return username != null ? username : claims.getSubject();
    }

    private Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}