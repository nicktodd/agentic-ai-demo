package com.example.portfolio.model;

import java.math.BigDecimal;

/** The value of one holding, expressed in the portfolio's base currency. */
public record HoldingValuation(
    String ticker,
    BigDecimal quantity,
    BigDecimal price,
    String priceCurrency,
    BigDecimal value,
    String valueCurrency) {}
