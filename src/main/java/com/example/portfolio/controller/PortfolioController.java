package com.example.portfolio.controller;

import com.example.portfolio.model.Holding;
import com.example.portfolio.model.Portfolio;
import com.example.portfolio.model.PortfolioValuation;
import com.example.portfolio.service.PortfolioService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

  private final PortfolioService portfolioService;

  public PortfolioController(PortfolioService portfolioService) {
    this.portfolioService = portfolioService;
  }

  @GetMapping
  public List<Portfolio> list() {
    return portfolioService.findAll();
  }

  @GetMapping("/{id}")
  public Portfolio get(@PathVariable Long id) {
    return portfolioService.findById(id);
  }

  @GetMapping("/{id}/holdings")
  public List<Holding> holdings(@PathVariable Long id) {
    return portfolioService.holdingsFor(id);
  }

  @PostMapping("/{id}/holdings")
  @ResponseStatus(HttpStatus.CREATED)
  public Holding addHolding(@PathVariable Long id, @RequestBody NewHolding request) {
    return portfolioService.addHolding(id, request.ticker(), request.quantity());
  }

  @DeleteMapping("/{id}/holdings/{holdingId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeHolding(@PathVariable Long id, @PathVariable Long holdingId) {
    portfolioService.removeHolding(id, holdingId);
  }

  /** Portfolio value in the portfolio's own base currency. */
  @GetMapping("/{id}/value")
  public PortfolioValuation value(@PathVariable Long id) {
    return portfolioService.value(id);
  }

  /** Request body for adding a holding. */
  public record NewHolding(String ticker, BigDecimal quantity) {}
}
