---
title: "Top N holdings by value for a portfolio"
labels: enhancement, breakout
---

## Summary

The portfolio dashboard shows the biggest positions first.

## Proposed API

```
GET /api/portfolios/{id}/holdings/top?n=3
```

Returns the holdings valued in the portfolio's base currency, sorted by `value`
descending, limited to `n` (default 5, must be between 1 and 50 or 400).

## Acceptance criteria

- [ ] Reuse `PortfolioService.value(...)` rather than re-implementing valuation.
- [ ] Sorting uses `BigDecimal.compareTo`, never `doubleValue()`.
- [ ] Unit test the ordering and the limit; `@WebMvcTest` for the default `n` and the
      400 case.
- [ ] `mvn -B verify` passes.
