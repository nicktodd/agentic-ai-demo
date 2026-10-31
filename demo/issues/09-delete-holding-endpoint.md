---
title: "Remove a holding from a portfolio"
labels: enhancement, breakout
---

## Summary

Operations need to remove a position that was booked by mistake.

## Proposed API

```
DELETE /api/portfolios/{portfolioId}/holdings/{holdingId}
```

- 204 No Content on success.
- 404 if the portfolio does not exist, or the holding does not exist **or belongs to a
  different portfolio** (do not leak that it exists elsewhere).

## Acceptance criteria

- [ ] Service method `removeHolding(Long portfolioId, Long holdingId)`.
- [ ] Unit tests for success, unknown holding and wrong-portfolio cases.
- [ ] `@WebMvcTest` asserting 204 and 404.
- [ ] `mvn -B verify` passes.
