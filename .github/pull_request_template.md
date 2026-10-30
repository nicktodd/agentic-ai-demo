## What

Closes #

## How

-

## Testing

- [ ] `mvn -B verify` passes locally (tests, Spotless, JaCoCo 85% gate)
- [ ] New behaviour has a test

## Review checklist (humans and agents)

- [ ] Money uses `BigDecimal` with an explicit `RoundingMode`
- [ ] Constructor injection, no field `@Autowired`
- [ ] No secrets, no changes to `infra/` or `Jenkinsfile` unless the issue asks
