package com.example.authUtils;

/** Identity established from the validated NestJS access token. */
public record AuthenticatedUser(
        Long userId,
        String username,
        String actorType,
        String role,
        String tokenId,
        String sessionId) {

    public boolean isClient() {
        return "client".equalsIgnoreCase(actorType);
    }

    public boolean isEmployee() {
        return "employee".equalsIgnoreCase(actorType);
    }
}
