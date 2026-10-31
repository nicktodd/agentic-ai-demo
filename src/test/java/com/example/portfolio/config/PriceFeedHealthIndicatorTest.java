package com.example.portfolio.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.portfolio.repository.PriceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class PriceFeedHealthIndicatorTest {

  @Mock private PriceRepository priceRepository;

  @InjectMocks private PriceFeedHealthIndicator healthIndicator;

  @Test
  void reportsUpWhenPricesExist() {
    when(priceRepository.count()).thenReturn(5L);

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("prices", 5L);
  }

  @Test
  void reportsDownWhenPriceFeedIsEmpty() {
    when(priceRepository.count()).thenReturn(0L);

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("reason", "No prices available in feed");
  }
}
