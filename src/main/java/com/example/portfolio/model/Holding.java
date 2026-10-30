package com.example.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** A position in a single instrument held inside a portfolio. */
@Entity
public class Holding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long portfolioId;
  private String ticker;

  @Column(precision = 19, scale = 4)
  private BigDecimal quantity;

  protected Holding() {}

  public Holding(Long portfolioId, String ticker, BigDecimal quantity) {
    this.portfolioId = portfolioId;
    this.ticker = ticker;
    this.quantity = quantity;
  }

  public Long getId() {
    return id;
  }

  public Long getPortfolioId() {
    return portfolioId;
  }

  public String getTicker() {
    return ticker;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }
}
