---
title: "Validate the add-holding request body with @Valid"
labels: enhancement, breakout
---

## Summary

`POST /api/portfolios/{id}/holdings` accepts anything: a blank ticker, a zero or negative
quantity, or a missing body field, and fails later with a 500 or stores bad data.

## Acceptance criteria

- [ ] `PortfolioController.NewHolding` fields are annotated with Bean Validation:
      `@NotBlank` ticker, `@NotNull @Positive` quantity (a `@Digits` fraction limit of 4 is
      a bonus).
- [ ] The controller method parameter is annotated `@Valid`.
- [ ] Invalid input returns **400** with a `ProblemDetail` body (Spring's default
      `MethodArgumentNotValidException` handling is fine, or pair with #5).
- [ ] `@WebMvcTest` cases for: blank ticker, negative quantity, valid request → 201.
- [ ] `mvn -B verify` passes.

## Hints

`spring-boot-starter-validation` is already on the classpath. Records support annotations
on their components: `record NewHolding(@NotBlank String ticker, ...)`.
