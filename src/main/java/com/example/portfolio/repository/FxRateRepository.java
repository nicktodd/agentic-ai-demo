package com.example.portfolio.repository;

import com.example.portfolio.model.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRateRepository extends JpaRepository<FxRate, String> {}
