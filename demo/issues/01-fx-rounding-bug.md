---
title: "Bug: FX conversion is off by a penny on some amounts"
labels: bug, demo, stage-1-general-ai
---

## Summary

Converted values from `FxService.convert` are sometimes 0.01 lower than expected.
The nightly reconciliation against the custodian flags the **Euro Income** portfolio
(id 3) as a one-cent break.

## Steps to reproduce

Start the app (`mvn spring-boot:run`) and call:

```
GET /api/fx/convert?amount=100&from=USD&to=GBP
```

**Actual:** `"converted": 78.64`
**Expected:** `78.65` (100 / 1.2715 = 78.6472… → 78.65 with HALF_UP)

Also:

```
GET /api/fx/convert?amount=250&from=GBP&to=USD
```

**Actual:** `317.87`  **Expected:** `317.88` (250 × 1.2715 = 317.875 → 317.88)

And `GET /api/portfolios/3/value` reports the HSBA line as `41225.55` EUR; the
correct value is `41225.56`.

## Notes

- The existing `FxServiceTest` uses round rates (1.25) and passes, so CI is green.
- Our repo convention (see `.github/copilot-instructions.md`) is `BigDecimal` for all
  money with an explicit `RoundingMode`.

## Acceptance criteria

- [ ] `FxService.convert` performs the arithmetic in `BigDecimal` end to end
      (no `double` anywhere in the method) and rounds with `RoundingMode.HALF_UP` to 2 dp.
- [ ] A new unit test reproduces the examples above with the real seed rates
      (GBP 1.2715, EUR 1.0845) and would have failed before the fix.
- [ ] `mvn -B verify` passes.
