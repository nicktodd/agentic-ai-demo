package com.example.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.portfolio.service.FxService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** End-to-end tests against the real application with the seeded H2 data. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PortfolioValuationApplicationTests {

  @Autowired private TestRestTemplate rest;
  @Autowired private FxService fxService;

  @Test
  void contextLoadsAndHealthIsUp() {
    ResponseEntity<Map> response = rest.getForEntity("/actuator/health", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "UP");
  }

  @Test
  void listsSeededPortfolios() {
    ResponseEntity<Map[]> response = rest.getForEntity("/api/portfolios", Map[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(3);
  }

  @Test
  void valuesUsTechPortfolioInUsd() {
    // 1000 * 189.84 + 400 * 415.26 + 75 * 1180.05 = 189840 + 166104 + 88503.75
    ResponseEntity<Map> response = rest.getForEntity("/api/portfolios/2/value", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("currency", "USD");
    assertThat(new BigDecimal(response.getBody().get("totalValue").toString()))
        .isEqualByComparingTo("444447.75");
  }

  @Test
  void valuesUsTechPortfolioInRequestedEur() {
    ResponseEntity<Map> response =
        rest.getForEntity("/api/portfolios/2/value?currency=EUR", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("currency", "EUR");
    List<?> holdings = (List<?>) response.getBody().get("holdings");
    assertThat(holdings).hasSize(3);
    Map<String, BigDecimal> seededUsdValues =
        Map.of(
            "AAPL", new BigDecimal("189840"),
            "MSFT", new BigDecimal("166104"),
            "NVDA", new BigDecimal("88503.75"));
    BigDecimal totalFromHoldings = BigDecimal.ZERO;
    for (Object item : holdings) {
      Map<?, ?> holding = (Map<?, ?>) item;
      assertThat(holding.get("valueCurrency")).isEqualTo("EUR");
      BigDecimal value = new BigDecimal(holding.get("value").toString());
      assertThat(value)
          .isEqualByComparingTo(
              fxService.convert(seededUsdValues.get(holding.get("ticker")), "USD", "EUR"));
      totalFromHoldings = totalFromHoldings.add(value);
    }
    assertThat(new BigDecimal(response.getBody().get("totalValue").toString()))
        .isEqualByComparingTo(totalFromHoldings);
  }

  @Test
  void unknownPortfolioReturns404() {
    ResponseEntity<String> response = rest.getForEntity("/api/portfolios/99", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void convertsFxViaEndpoint() {
    ResponseEntity<Map> response =
        rest.getForEntity("/api/fx/convert?amount=100&from=GBP&to=USD", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new BigDecimal(response.getBody().get("converted").toString()))
        .isEqualByComparingTo("127.15");
  }
}
