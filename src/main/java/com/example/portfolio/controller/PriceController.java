package com.example.portfolio.controller;

import com.example.portfolio.model.Price;
import com.example.portfolio.service.PricingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

  private final PricingService pricingService;

  public PriceController(PricingService pricingService) {
    this.pricingService = pricingService;
  }

  @GetMapping
  public List<Price> list() {
    return pricingService.findAll();
  }

  @GetMapping("/{ticker}")
  public Price get(@PathVariable String ticker) {
    return pricingService.priceFor(ticker);
  }
}
