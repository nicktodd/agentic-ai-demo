---
title: "New query endpoint: which portfolios hold a given ticker?"
labels: enhancement, breakout
---

## Summary

Risk wants to answer "who holds NVDA?" across all portfolios.

## Proposed API

```
GET /api/holdings?ticker=NVDA
```

Returns a JSON array of `{ portfolioId, portfolioName, ticker, quantity }`.
An unknown ticker returns an empty array (200), not 404. Ticker is case-insensitive.

## Acceptance criteria

- [ ] `HoldingRepository.findByTicker(String)` derived query.
- [ ] New service method and a new `HoldingController` (keep `PortfolioController` focused).
- [ ] A response `record` in `model/` – don't return the entity directly.
- [ ] `@WebMvcTest` for the controller and a `@SpringBootTest` case using the seed data
      (`HSBA` is held by portfolios 1 and 3).
- [ ] `mvn -B verify` passes.
