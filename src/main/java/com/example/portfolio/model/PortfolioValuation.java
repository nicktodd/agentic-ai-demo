package com.example.portfolio.model;

import java.math.BigDecimal;
import java.util.List;

/** The total value of a portfolio plus a per-holding breakdown. */
public record PortfolioValuation(
    Long portfolioId,
    String name,
    String currency,
    BigDecimal totalValue,
    List<HoldingValuation> holdings) {}
