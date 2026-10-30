---
title: "Custom Actuator health indicator for the price feed"
labels: enhancement, good first issue, breakout
---

## Summary

The ECS target group health check hits `/actuator/health`. Today it only reports that the
JVM and H2 are up. If the price table is empty the service is "healthy" but useless.

## Acceptance criteria

- [ ] New `config/PriceFeedHealthIndicator implements HealthIndicator`.
- [ ] Reports `UP` with a `prices` detail (count) when at least one price exists,
      otherwise `DOWN` with a `reason`.
- [ ] Appears under `components.priceFeed` in `/actuator/health` (details are already
      enabled in `application.yml`).
- [ ] Unit test both branches with a mocked `PriceRepository`; extend the existing
      `@SpringBootTest` health test to assert the component is present.
- [ ] `mvn -B verify` passes.
