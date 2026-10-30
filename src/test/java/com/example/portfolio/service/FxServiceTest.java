package com.example.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.portfolio.model.FxRate;
import com.example.portfolio.model.NotFoundException;
import com.example.portfolio.repository.FxRateRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FxServiceTest {

  @Mock private FxRateRepository fxRateRepository;

  private FxService fxService;

  @BeforeEach
  void setUp() {
    fxService = new FxService(fxRateRepository);
  }

  @Test
  void sameCurrencyIsIdentity() {
    assertThat(fxService.convert(new BigDecimal("100"), "USD", "USD"))
        .isEqualByComparingTo("100.00");
  }

  @Test
  void convertsGbpToUsd() {
    when(fxRateRepository.findById("GBP"))
        .thenReturn(Optional.of(new FxRate("GBP", new BigDecimal("1.25"))));

    assertThat(fxService.convert(new BigDecimal("100"), "GBP", "USD"))
        .isEqualByComparingTo("125.00");
  }

  @Test
  void convertsUsdToEur() {
    when(fxRateRepository.findById("EUR"))
        .thenReturn(Optional.of(new FxRate("EUR", new BigDecimal("1.25"))));

    assertThat(fxService.convert(new BigDecimal("125"), "USD", "EUR"))
        .isEqualByComparingTo("100.00");
  }

  @Test
  void unknownCurrencyThrowsNotFound() {
    when(fxRateRepository.findById("CHF")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fxService.convert(BigDecimal.TEN, "CHF", "USD"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("CHF");
  }
}
