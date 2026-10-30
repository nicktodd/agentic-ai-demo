package com.example.portfolio.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.portfolio.model.Price;
import com.example.portfolio.service.PricingService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PriceController.class)
class PriceControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PricingService pricingService;

  @Test
  void listsPrices() throws Exception {
    when(pricingService.findAll())
        .thenReturn(List.of(new Price("AAPL", new BigDecimal("189.84"), "USD")));

    mockMvc
        .perform(get("/api/prices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ticker").value("AAPL"));
  }

  @Test
  void getsSinglePrice() throws Exception {
    when(pricingService.priceFor("AAPL"))
        .thenReturn(new Price("AAPL", new BigDecimal("189.84"), "USD"));

    mockMvc
        .perform(get("/api/prices/AAPL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("USD"));
  }
}
