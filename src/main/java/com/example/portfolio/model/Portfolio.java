package com.example.portfolio.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A named portfolio with a base (reporting) currency. */
@Entity
public class Portfolio {

  @Id private Long id;
  private String name;
  private String baseCurrency;

  protected Portfolio() {}

  public Portfolio(Long id, String name, String baseCurrency) {
    this.id = id;
    this.name = name;
    this.baseCurrency = baseCurrency;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }
}
