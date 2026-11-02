package com.example.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.portfolio.model.Holding;
import com.example.portfolio.model.NotFoundException;
import com.example.portfolio.model.Portfolio;
import com.example.portfolio.model.PortfolioValuation;
import com.example.portfolio.model.Price;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

  @Mock private PortfolioRepository portfolioRepository;
  @Mock private HoldingRepository holdingRepository;
  @Mock private PricingService pricingService;
  @Mock private FxService fxService;

  private PortfolioService portfolioService;

  @BeforeEach
  void setUp() {
    portfolioService =
        new PortfolioService(portfolioRepository, holdingRepository, pricingService, fxService);
  }

  @Test
  void valuesHoldingsInBaseCurrency() {
    Portfolio portfolio = new Portfolio(1L, "UK Growth", "GBP");
    when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
    when(holdingRepository.findByPortfolioId(1L))
        .thenReturn(
            List.of(
                new Holding(1L, "HSBA", new BigDecimal("100")),
                new Holding(1L, "AAPL", new BigDecimal("10"))));
    when(pricingService.priceFor("HSBA")).thenReturn(new Price("HSBA", new BigDecimal("7"), "GBP"));
    when(pricingService.priceFor("AAPL"))
        .thenReturn(new Price("AAPL", new BigDecimal("200"), "USD"));
    when(fxService.convert(new BigDecimal("700"), "GBP", "GBP"))
        .thenReturn(new BigDecimal("700.00"));
    when(fxService.convert(new BigDecimal("2000"), "USD", "GBP"))
        .thenReturn(new BigDecimal("1600.00"));

    PortfolioValuation valuation = portfolioService.value(1L);

    assertThat(valuation.currency()).isEqualTo("GBP");
    assertThat(valuation.totalValue()).isEqualByComparingTo("2300.00");
    assertThat(valuation.holdings()).hasSize(2);
    assertThat(valuation.holdings().get(1).value()).isEqualByComparingTo("1600.00");
  }

  @Test
  void unknownPortfolioThrows() {
    when(portfolioRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> portfolioService.value(42L)).isInstanceOf(NotFoundException.class);
  }
}
