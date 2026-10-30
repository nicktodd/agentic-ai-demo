---
title: "Add a @ControllerAdvice that maps errors to ProblemDetail"
labels: enhancement, good first issue, breakout
---

## Summary

Error responses are inconsistent. `NotFoundException` relies on `ResponseStatusException`,
validation failures use Spring's default shape, and an unsupported currency currently
surfaces as a 500. We want one consistent RFC 9457 `ProblemDetail` body.

## Acceptance criteria

- [ ] New class `controller/GlobalExceptionHandler` annotated `@RestControllerAdvice`.
- [ ] Handles `NotFoundException` → 404, `IllegalArgumentException` → 400,
      `MethodArgumentNotValidException` → 400 with a list of field errors in a
      `errors` property.
- [ ] Each response body is a `ProblemDetail` with `title`, `status`, `detail` and the
      request `instance`.
- [ ] Unit test the handler directly **and** add one `@WebMvcTest` case proving the JSON
      shape for a 404.
- [ ] `mvn -B verify` passes.
