package com.ihit.stock.repository;

import com.ihit.stock.model.TradingCode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TradingCodeRepository extends JpaRepository<TradingCode, Long> {
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Page<TradingCode> findAllByOrderByCodeAsc(Pageable pageable);
    List<TradingCode> findAllByOrderByCodeAsc();

    @Query("SELECT t.code FROM TradingCode t ORDER BY t.code")
    List<String> findDistinctTradingCodes();

    public interface TradingCodeProjection {
        String getCode();
        String getCompanyName();
        String getSector();
        String getMarketCategory();
    }

    @Query(value = "SELECT tc.code as code, c.company_name as company_name, " +
           "c.sector as sector, c.market_category as market_category FROM trading_codes tc " +
           "LEFT JOIN company c ON c.trading_code = tc.code ORDER BY tc.code", nativeQuery = true)
    List<TradingCodeProjection> findTradingCodeDetails();

    @Query(value = "SELECT tc.code as code, c.company_name as company_name, " +
           "c.sector as sector, c.market_category as market_category FROM trading_codes tc " +
           "INNER JOIN company c ON c.trading_code = tc.code ORDER BY tc.code", nativeQuery = true)
    List<TradingCodeProjection> findFundamentalTradingCodeDetails();

    Page<TradingCode> findAllByCodeIgnoreCaseContainingOrderByCodeAsc(String code, Pageable pageable);
}
