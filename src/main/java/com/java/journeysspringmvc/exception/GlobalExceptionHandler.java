/*
 * Turns exceptions thrown anywhere in the controllers into consistent JSON error responses.
 *
 * Every error body uses Spring's ProblemDetail (RFC 9457) format, e.g.
 *   {"status": 404, "title": "Not Found", "detail": "No tour found with id ...", "instance": "/api/v1/tours/..."}
 * Mappings:
 *   NotFoundException                 -> 404
 *   BadRequestException               -> 400 (business rule broken, e.g. a guideId that isn't a guide,
 *                                             or sorting by a field that isn't allowed)
 *   DataIntegrityViolationException   -> 409 (unique constraint, e.g. a duplicate tour slug)
 * Extending ResponseEntityExceptionHandler also covers Spring MVC's own errors, such as failed
 * @Validated bodies, unreadable JSON or a malformed UUID in the path, which all become 400s.
 * Anything else is a bug and stays a 500.
 */
package com.java.journeysspringmvc.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource conflicts with an existing one");
    }
}
