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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
  void valuesHoldingsInRequestedCurrency() {
    Portfolio portfolio = new Portfolio(2L, "US Tech", "USD");
    when(portfolioRepository.findById(2L)).thenReturn(Optional.of(portfolio));
    when(holdingRepository.findByPortfolioId(2L))
        .thenReturn(
            List.of(
                new Holding(2L, "AAPL", new BigDecimal("2")),
                new Holding(2L, "MSFT", new BigDecimal("3"))));
    when(pricingService.priceFor("AAPL")).thenReturn(new Price("AAPL", new BigDecimal("10"), "USD"));
    when(pricingService.priceFor("MSFT"))
        .thenReturn(new Price("MSFT", new BigDecimal("20"), "USD"));
    when(fxService.convert(new BigDecimal("20"), "USD", "GBP"))
        .thenReturn(new BigDecimal("15.00"));
    when(fxService.convert(new BigDecimal("60"), "USD", "GBP"))
        .thenReturn(new BigDecimal("45.00"));

    PortfolioValuation valuation = portfolioService.value(2L, "gbp");

    assertThat(valuation.currency()).isEqualTo("GBP");
    assertThat(valuation.totalValue()).isEqualByComparingTo("60.00");
    assertThat(valuation.holdings()).hasSize(2);
    assertThat(valuation.holdings().get(0).value()).isEqualByComparingTo("15.00");
    assertThat(valuation.holdings().get(0).valueCurrency()).isEqualTo("GBP");
    assertThat(valuation.holdings().get(1).value()).isEqualByComparingTo("45.00");
    assertThat(valuation.holdings().get(1).valueCurrency()).isEqualTo("GBP");
  }

  @Test
  void unsupportedCurrencyThrowsBadRequestNamingCurrency() {
    when(portfolioRepository.findById(2L))
        .thenReturn(Optional.of(new Portfolio(2L, "US Tech", "USD")));

    assertThatThrownBy(() -> portfolioService.value(2L, "chf"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unsupported currency: chf")
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void unknownPortfolioThrows() {
    when(portfolioRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> portfolioService.value(42L)).isInstanceOf(NotFoundException.class);
  }
}
