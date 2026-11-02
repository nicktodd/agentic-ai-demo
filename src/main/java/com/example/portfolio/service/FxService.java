package com.example.portfolio.service;

import com.example.portfolio.model.FxRate;
import com.example.portfolio.model.NotFoundException;
import com.example.portfolio.repository.FxRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Converts monetary amounts between currencies. All rates are stored against USD, so a conversion
 * from GBP to EUR goes GBP -> USD -> EUR.
 */
@Service
public class FxService {

  public static final Set<String> SUPPORTED = Set.of("USD", "GBP", "EUR");
  private static final String PIVOT = "USD";

  private final FxRateRepository fxRateRepository;

  public FxService(FxRateRepository fxRateRepository) {
    this.fxRateRepository = fxRateRepository;
  }

  /** Converts {@code amount} from one currency to another, rounded to 2 decimal places. */
  public BigDecimal convert(BigDecimal amount, String from, String to) {
    if (from.equals(to)) {
      return amount.setScale(2, RoundingMode.HALF_UP);
    }
    return amount.multiply(usdRate(from)).divide(usdRate(to), 2, RoundingMode.HALF_UP);
  }

  private BigDecimal usdRate(String currency) {
    if (PIVOT.equals(currency)) {
      return BigDecimal.ONE;
    }
    return fxRateRepository
        .findById(currency)
        .map(FxRate::getRate)
        .orElseThrow(() -> new NotFoundException("No FX rate for currency " + currency));
  }
}
