package com.example.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** Exchange rate quoted as 1 unit of {@code baseCurrency} = {@code rate} units of USD. */
@Entity
public class FxRate {

  @Id private String baseCurrency;

  @Column(precision = 19, scale = 6)
  private BigDecimal rate;

  protected FxRate() {}

  public FxRate(String baseCurrency, BigDecimal rate) {
    this.baseCurrency = baseCurrency;
    this.rate = rate;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public BigDecimal getRate() {
    return rate;
  }
}
