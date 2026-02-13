package com.rgbconsulting.jwt.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 *
 * @author sergi
 */
@RestControllerAdvice
public class JwtNotFoundAdvice {

    @ExceptionHandler(JwtNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String employeeNotFoundHandler(JwtNotFoundException ex) {
        return ex.getMessage();
    }
}
