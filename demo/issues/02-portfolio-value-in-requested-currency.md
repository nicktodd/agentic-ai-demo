---
title: "Feature: value a portfolio in a requested currency"
labels: enhancement, demo, stage-3-agentic
---

## Summary

`GET /api/portfolios/{id}/value` only returns the value in the portfolio's base currency.
Clients in Group Reporting need the same breakdown in GBP, USD or EUR regardless of the
portfolio's base currency.

## Proposed API

```
GET /api/portfolios/{id}/value?currency=EUR
```

- `currency` is optional. When omitted, behave exactly as today (base currency).
- When supplied, every `HoldingValuation.value` and the `totalValue` are expressed in the
  requested currency, and the `currency` field in the response reflects it.
- Currency codes are case-insensitive (`eur` works) and must be one of `GBP`, `USD`, `EUR`.
- An unsupported currency (e.g. `CHF`) returns **400 Bad Request** with a `ProblemDetail`
  body whose `detail` names the bad currency. It must not return 500.

## Example

```
GET /api/portfolios/2/value?currency=GBP

{
  "portfolioId": 2,
  "name": "US Tech",
  "currency": "GBP",
  "totalValue": 349546.02,
  "holdings": [ ... each value in GBP ... ]
}
```

(Exact figures depend on the fix for #1; the test should assert against values derived
from the seed data, not hard-coded guesses.)

## Implementation notes

- Add the optional parameter in `PortfolioController`, pass it through to
  `PortfolioService.value(Long, String)`; keep the existing `value(Long)` delegating to it.
- Validation of the currency belongs in the service, using `FxService.SUPPORTED`.
- Follow `.github/copilot-instructions.md`: constructor injection, `BigDecimal`, tests.

## Acceptance criteria

- [ ] New `@WebMvcTest` cases: default behaviour unchanged; `?currency=gbp` passes `GBP`
      to the service; unsupported currency gives 400.
- [ ] New `PortfolioServiceTest` case proving the holdings are converted into the
      requested currency.
- [ ] A `@SpringBootTest` case calling the real endpoint with `?currency=EUR` on portfolio 2.
- [ ] `mvn -B verify` passes, **including the 85% coverage gate**.
