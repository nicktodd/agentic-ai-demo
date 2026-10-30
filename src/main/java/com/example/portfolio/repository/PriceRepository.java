package com.example.portfolio.repository;

import com.example.portfolio.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRepository extends JpaRepository<Price, String> {}
