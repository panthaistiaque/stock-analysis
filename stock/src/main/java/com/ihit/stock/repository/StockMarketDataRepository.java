package com.ihit.stock.repository;

import com.ihit.stock.model.StockMarketData;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMarketDataRepository extends JpaRepository<StockMarketData, Long> {

    Optional<StockMarketData> findByDateAndTradingCodeIgnoreCase(LocalDate date, String tradingCode);

    List<StockMarketData> findAllByOrderByDateDescTradingCodeAsc();

    long deleteByTradingCodeIgnoreCase(String tradingCode);

    @Query("SELECT s FROM StockMarketData s WHERE " +
           "(CAST(:tradingCode AS String) IS NULL OR :tradingCode = '' OR UPPER(s.tradingCode) LIKE UPPER(CONCAT('%', :tradingCode, '%'))) AND " +
           "(CAST(:fromDate AS LocalDate) IS NULL OR s.date >= :fromDate) AND " +
           "(CAST(:toDate AS LocalDate) IS NULL OR s.date <= :toDate)")
    Page<StockMarketData> findWithFilters(
            @Param("tradingCode") String tradingCode,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
