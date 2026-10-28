/*
 * Thrown when a call to Keycloak's admin API fails for reasons other than "not found"
 * (Keycloak down, misconfigured service account, unexpected error).
 *
 * GlobalExceptionHandler maps it to 502 Bad Gateway: the request was fine, but a service
 * this API depends on couldn't complete it.
 */
package com.java.journeysspringmvc.exception;

public class IdentityProviderException extends RuntimeException {
    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
