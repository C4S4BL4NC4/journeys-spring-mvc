/*
 * Thrown by controllers when the requested tour or user doesn't exist.
 *
 * Services return Optional.empty() or false for "not found"; controllers convert that into
 * this exception, and GlobalExceptionHandler turns it into a 404 response with the message
 * as the "detail" field.
 */
package com.java.journeysspringmvc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
