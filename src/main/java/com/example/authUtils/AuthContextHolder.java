package com.example.authUtils;

import org.springframework.security.access.AccessDeniedException;

/** Request-scoped identity extracted from the NestJS access token. */
public final class AuthContextHolder {
    private static final ThreadLocal<AuthenticatedUser> CONTEXT = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(AuthenticatedUser user) {
        CONTEXT.set(user);
    }

    public static AuthenticatedUser get() {
        return CONTEXT.get();
    }

    public static AuthenticatedUser requireAuthenticatedUser() {
        AuthenticatedUser user = get();
        if (user == null) {
            throw new AccessDeniedException("No authenticated user is available");
        }
        return user;
    }

    public static Long requireClientId() {
        AuthenticatedUser user = requireAuthenticatedUser();
        if (!user.isClient()) {
            throw new AccessDeniedException("Only client tokens may access this resource");
        }
        return user.userId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
