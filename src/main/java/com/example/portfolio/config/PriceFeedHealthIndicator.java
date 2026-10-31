package com.example.portfolio.config;

import com.example.portfolio.repository.PriceRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PriceFeedHealthIndicator implements HealthIndicator {

  private final PriceRepository priceRepository;

  public PriceFeedHealthIndicator(PriceRepository priceRepository) {
    this.priceRepository = priceRepository;
  }

  @Override
  public Health health() {
    long count = priceRepository.count();
    if (count > 0) {
      return Health.up().withDetail("prices", count).build();
    }
    return Health.down().withDetail("reason", "No prices available in feed").build();
  }
}
