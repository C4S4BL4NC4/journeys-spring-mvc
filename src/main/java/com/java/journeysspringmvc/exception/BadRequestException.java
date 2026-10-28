/*
 * Thrown when a request is well-formed and passes bean validation but still can't be
 * honoured, e.g. a guideId that doesn't belong to a guide or an unsupported sort field.
 *
 * GlobalExceptionHandler maps it to a 400 with the message as the "detail" field. Using a
 * dedicated type (rather than IllegalArgumentException) means genuine bugs elsewhere still
 * surface as 500s instead of being blamed on the client.
 */
package com.java.journeysspringmvc.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
