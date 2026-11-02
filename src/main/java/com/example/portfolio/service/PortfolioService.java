package com.example.portfolio.service;

import com.example.portfolio.model.Holding;
import com.example.portfolio.model.HoldingValuation;
import com.example.portfolio.model.NotFoundException;
import com.example.portfolio.model.Portfolio;
import com.example.portfolio.model.PortfolioValuation;
import com.example.portfolio.model.Price;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Business logic for portfolios: listing, holdings and valuation. */
@Service
public class PortfolioService {

  private final PortfolioRepository portfolioRepository;
  private final HoldingRepository holdingRepository;
  private final PricingService pricingService;
  private final FxService fxService;

  public PortfolioService(
      PortfolioRepository portfolioRepository,
      HoldingRepository holdingRepository,
      PricingService pricingService,
      FxService fxService) {
    this.portfolioRepository = portfolioRepository;
    this.holdingRepository = holdingRepository;
    this.pricingService = pricingService;
    this.fxService = fxService;
  }

  public List<Portfolio> findAll() {
    return portfolioRepository.findAll();
  }

  public Portfolio findById(Long id) {
    return portfolioRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("No portfolio with id " + id));
  }

  public List<Holding> holdingsFor(Long portfolioId) {
    findById(portfolioId);
    return holdingRepository.findByPortfolioId(portfolioId);
  }

  public Holding addHolding(Long portfolioId, String ticker, BigDecimal quantity) {
    findById(portfolioId);
    pricingService.priceFor(ticker);
    return holdingRepository.save(new Holding(portfolioId, ticker.toUpperCase(), quantity));
  }

  /** Values every holding in the portfolio's base currency and sums them. */
  public PortfolioValuation value(Long portfolioId) {
    return value(portfolioId, null);
  }

  /**
   * Values every holding in the requested currency, defaulting to the portfolio's base currency.
   */
  public PortfolioValuation value(Long portfolioId, String requestedCurrency) {
    Portfolio portfolio = findById(portfolioId);
    String currency =
        requestedCurrency == null
            ? portfolio.getBaseCurrency()
            : requestedCurrency.toUpperCase(Locale.ROOT);
    if (!FxService.SUPPORTED.contains(currency)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported currency: " + requestedCurrency);
    }

    List<HoldingValuation> valued =
        holdingRepository.findByPortfolioId(portfolioId).stream()
            .map(holding -> valueHolding(holding, currency))
            .toList();

    BigDecimal total =
        valued.stream().map(HoldingValuation::value).reduce(BigDecimal.ZERO, BigDecimal::add);

    return new PortfolioValuation(portfolio.getId(), portfolio.getName(), currency, total, valued);
  }

  private HoldingValuation valueHolding(Holding holding, String targetCurrency) {
    Price price = pricingService.priceFor(holding.getTicker());
    BigDecimal localValue = holding.getQuantity().multiply(price.getPrice());
    BigDecimal converted = fxService.convert(localValue, price.getCurrency(), targetCurrency);
    return new HoldingValuation(
        holding.getTicker(),
        holding.getQuantity(),
        price.getPrice(),
        price.getCurrency(),
        converted,
        targetCurrency);
  }
}
