package com.ihit.stock.repository;

import com.ihit.stock.model.TradingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TradingCodeRepository extends JpaRepository<TradingCode, Long> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<TradingCode> findAllByOrderByCodeAsc();

    @Query("""
                SELECT DISTINCT s.tradingCode
                FROM StockMarketData s
                ORDER BY s.tradingCode
            """)
    List<String> findDistinctTradingCodes();
}
