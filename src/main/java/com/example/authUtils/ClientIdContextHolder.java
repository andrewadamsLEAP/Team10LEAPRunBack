package com.example.authUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

/**
 * This allows services and controllers to access the current clientId without
 * passing it through method parameters.
 * 
 * Use ClientIdContextHolder.getClientId()
 */

public class ClientIdContextHolder {
    private static final Logger logger = LoggerFactory.getLogger(ClientIdContextHolder.class);
    private static final ThreadLocal<Long> clientIdThreadLocal = new ThreadLocal<>();
    
    /**
     * Set the clientId for the current thread/request
     */
    public static void setClientId(Long clientId) {
        logger.debug("Setting clientId in context: {}", clientId);
        clientIdThreadLocal.set(clientId);
    }
    
    /**
     * Get the clientId for the current thread/request
     * 
     * @return the clientId, or null if not set
     */
    public static Long getClientId() {
        Long clientId = clientIdThreadLocal.get();
        logger.debug("Getting clientId from context: {}", clientId);
        return clientId;
    }

    public static void requireClient(Long requestedClientId) {
        Long authenticatedClientId = getClientId();
        if (authenticatedClientId == null || !authenticatedClientId.equals(requestedClientId)) {
            throw new AccessDeniedException("The authenticated client cannot access this client resource");
        }
    }
    
    /**
     * Clear the clientId from the current thread/request
     * Should be called in a finally block or filter cleanup
     */
    public static void clear() {
        logger.debug("Clearing clientId from context");
        clientIdThreadLocal.remove();
    }
}
