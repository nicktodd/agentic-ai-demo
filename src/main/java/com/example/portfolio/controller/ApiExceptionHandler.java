package com.example.portfolio.controller;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleResponseStatusException(ResponseStatusException exception) {
    return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
  }
}
