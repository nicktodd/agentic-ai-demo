---
title: "Tests: close the coverage gap around addHolding and cross-currency FX"
labels: tests, demo, good first issue
---

## Summary

The JaCoCo gate is set to 85% line coverage and the build currently sits at ~90%, so any
untested addition of more than a few lines fails CI. Two areas have no direct tests:

1. `PortfolioService.addHolding` and `PortfolioController.addHolding`
   (`POST /api/portfolios/{id}/holdings`) – the only coverage comes indirectly.
2. `FxService.convert` for a conversion where **neither** side is USD (e.g. GBP → EUR,
   which pivots through USD). Only USD legs are tested.

Open `target/site/jacoco/index.html` after `mvn verify` to see the red lines.

## Acceptance criteria

- [ ] `PortfolioServiceTest`: `addHolding` upper-cases the ticker, validates the portfolio
      and ticker exist, and saves the holding; unknown ticker → `NotFoundException`.
- [ ] `PortfolioControllerTest`: `POST /api/portfolios/1/holdings` returns 201 with the
      created holding as JSON.
- [ ] `FxServiceTest`: GBP → EUR with the seed rates (1.2715 / 1.0845).
- [ ] Raise `jacoco.minimum.line` in `pom.xml` to `0.92` and keep the build green.
