package com.example.portfolio.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when a portfolio, price or FX rate cannot be found. */
public class NotFoundException extends ResponseStatusException {

  public NotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }
}
