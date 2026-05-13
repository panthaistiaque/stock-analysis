package com.ihit.stock.repository;

import com.ihit.stock.model.StockMarketData;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMarketDataRepository extends JpaRepository<StockMarketData, Long> {

    Optional<StockMarketData> findByDateAndTradingCodeIgnoreCase(LocalDate date, String tradingCode);

    List<StockMarketData> findAllByOrderByDateDescTradingCodeAsc();

    long deleteByTradingCodeIgnoreCase(String tradingCode);
}
