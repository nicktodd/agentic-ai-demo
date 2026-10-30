package com.example.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** Latest known price for an instrument, in the instrument's trading currency. */
@Entity
public class Price {

  @Id private String ticker;

  @Column(precision = 19, scale = 6)
  private BigDecimal price;

  private String currency;

  protected Price() {}

  public Price(String ticker, BigDecimal price, String currency) {
    this.ticker = ticker;
    this.price = price;
    this.currency = currency;
  }

  public String getTicker() {
    return ticker;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public String getCurrency() {
    return currency;
  }
}
