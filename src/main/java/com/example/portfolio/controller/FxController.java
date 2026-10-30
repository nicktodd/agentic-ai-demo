package com.example.portfolio.controller;

import com.example.portfolio.service.FxService;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fx")
public class FxController {

  private final FxService fxService;

  public FxController(FxService fxService) {
    this.fxService = fxService;
  }

  /** Example: {@code GET /api/fx/convert?amount=100&from=GBP&to=EUR}. */
  @GetMapping("/convert")
  public Conversion convert(
      @RequestParam BigDecimal amount, @RequestParam String from, @RequestParam String to) {
    String fromCcy = from.toUpperCase();
    String toCcy = to.toUpperCase();
    return new Conversion(amount, fromCcy, fxService.convert(amount, fromCcy, toCcy), toCcy);
  }

  public record Conversion(BigDecimal amount, String from, BigDecimal converted, String to) {}
}
