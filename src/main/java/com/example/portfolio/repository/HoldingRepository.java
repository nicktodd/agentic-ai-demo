package com.example.portfolio.repository;

import com.example.portfolio.model.Holding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

  List<Holding> findByPortfolioId(Long portfolioId);
}
