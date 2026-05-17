package com.ihit.stock.repository;

import com.ihit.stock.model.MarketForecast;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarketForecastRepository extends JpaRepository<MarketForecast, Long> {

    interface ForecastCoverageProjection {
        String getTradingCode();
        Long getForecastRowCount();
        Long getVerifiedRowCount();
    }

    Optional<MarketForecast> findByTradingCodeIgnoreCaseAndForecastBaseDateAndTargetDate(
            String tradingCode,
            LocalDate forecastBaseDate,
            LocalDate targetDate);

    List<MarketForecast> findAllByTradingCodeIgnoreCaseOrderByForecastBaseDateDescTargetDateAsc(String tradingCode);

    long countByActualPriceIsNotNull();

    @Query("SELECT f.tradingCode AS tradingCode, COUNT(f) AS forecastRowCount, " +
           "SUM(CASE WHEN f.actualPrice IS NOT NULL THEN 1 ELSE 0 END) AS verifiedRowCount " +
           "FROM MarketForecast f GROUP BY f.tradingCode ORDER BY f.tradingCode")
    List<ForecastCoverageProjection> findForecastCoverageSummary();
}
