package com.example.portfolio.service;

import com.example.portfolio.model.NotFoundException;
import com.example.portfolio.model.Price;
import com.example.portfolio.repository.PriceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Looks up the latest price for an instrument. */
@Service
public class PricingService {

  private final PriceRepository priceRepository;

  public PricingService(PriceRepository priceRepository) {
    this.priceRepository = priceRepository;
  }

  public List<Price> findAll() {
    return priceRepository.findAll();
  }

  public Price priceFor(String ticker) {
    return priceRepository
        .findById(ticker.toUpperCase())
        .orElseThrow(() -> new NotFoundException("No price for ticker " + ticker));
  }
}
