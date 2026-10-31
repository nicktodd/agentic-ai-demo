package com.example.portfolio.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.portfolio.model.Holding;
import com.example.portfolio.model.NotFoundException;
import com.example.portfolio.model.Portfolio;
import com.example.portfolio.service.PortfolioService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer slice test: only the controller and MVC infrastructure are loaded. */
@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PortfolioService portfolioService;

  @Test
  void listsPortfolios() throws Exception {
    when(portfolioService.findAll())
        .thenReturn(
            List.of(new Portfolio(1L, "UK Growth", "GBP"), new Portfolio(2L, "US Tech", "USD")));

    mockMvc
        .perform(get("/api/portfolios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value("UK Growth"));
  }

  @Test
  void getUnknownPortfolioIs404() throws Exception {
    when(portfolioService.findById(99L)).thenThrow(new NotFoundException("No portfolio 99"));

    mockMvc.perform(get("/api/portfolios/99")).andExpect(status().isNotFound());
  }

  @Test
  void listsHoldings() throws Exception {
    when(portfolioService.holdingsFor(1L))
        .thenReturn(List.of(new Holding(1L, "HSBA", new BigDecimal("12000"))));

    mockMvc
        .perform(get("/api/portfolios/1/holdings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ticker").value("HSBA"));
  }

  @Test
  void removeHoldingReturns204() throws Exception {
    mockMvc.perform(delete("/api/portfolios/1/holdings/5")).andExpect(status().isNoContent());
  }

  @Test
  void removeUnknownHoldingIs404() throws Exception {
    doThrow(new NotFoundException("No holding 5")).when(portfolioService).removeHolding(1L, 5L);

    mockMvc.perform(delete("/api/portfolios/1/holdings/5")).andExpect(status().isNotFound());
  }
}
