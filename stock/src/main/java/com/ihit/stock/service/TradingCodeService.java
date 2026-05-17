package com.ihit.stock.service;

import com.ihit.stock.dto.CompanyFundamentalDto;
import com.ihit.stock.model.TradingCode;
import com.ihit.stock.repository.TradingCodeRepository;
import com.ihit.stock.service.scraper.CompanyScraperService;
import com.ihit.stock.service.scraper.MarketScraperServcie;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TradingCodeService {
    private final TradingCodeRepository repository;
    private final CompanyScraperService scraperService;
    private final CompanyService companyService;
    private final MarketScraperServcie marketScraperServcie;

    public TradingCodeService(TradingCodeRepository repository, CompanyScraperService scraperService,
            CompanyService companyService, MarketScraperServcie marketScraperServcie) {
        this.repository = repository;
        this.scraperService = scraperService;
        this.companyService = companyService;
        this.marketScraperServcie = marketScraperServcie;
    }

    @Transactional(readOnly = true)
    public Page<TradingCode> findAll(@Nullable String code, Pageable pageable) {
        if (code != null && !code.isBlank()) {
            return repository.findAllByCodeIgnoreCaseContainingOrderByCodeAsc(code, pageable);
        } else {
            return repository.findAllByOrderByCodeAsc(pageable);
        }
    }

    @Transactional(readOnly = true)
    public TradingCode findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trading code not found."));
    }

    @Transactional
    public TradingCode create(String code, String createdBy) {
        String cleanCode = cleanCode(code);
        if (repository.existsByCodeIgnoreCase(cleanCode)) {
            throw new IllegalArgumentException("Trading code already exists: " + cleanCode);
        }

        TradingCode tradingCode = new TradingCode();
        tradingCode.setCode(cleanCode);
        tradingCode.setCreationDate(LocalDateTime.now());
        tradingCode.setCreatedBy(cleanUserName(createdBy));
        return repository.save(tradingCode);
    }

    @Transactional
    public TradingCode update(Long id, String code) {
        TradingCode tradingCode = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trading code not found."));
        String cleanCode = cleanCode(code);
        if (repository.existsByCodeIgnoreCaseAndIdNot(cleanCode, id)) {
            throw new IllegalArgumentException("Trading code already exists: " + cleanCode);
        }
        tradingCode.setCode(cleanCode);
        return repository.save(tradingCode);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Trading code not found.");
        }
        repository.deleteById(id);
    }

    public String scrapeAndSave(Long id, String userName) {
        TradingCode tradingCode = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trading code not found."));
        scrapeAndSaveCode(tradingCode.getCode(), userName);
        return tradingCode.getCode();
    }

    public String scrapeAndSaveAll(String userName) {
        int saved = 0;
        List<String> failedCodes = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();

        for (TradingCode tradingCode : repository.findAllByOrderByCodeAsc()) {
            try {
                String code = tradingCode.getCode();
                // Scrape fundamental company information
                scrapeAndSaveCode(code, userName);
                // Scrape today's market archive data
                marketScraperServcie.scrapeByDateRange(code, today, today, userName);
                saved++;
            } catch (Exception e) {
                failedCodes.add(tradingCode.getCode());
            }
        }
        if (failedCodes.isEmpty()) {
            return saved + " company records scraped and saved.";
        }
        return saved + " company records scraped and saved. Failed: " + String.join(", ", failedCodes) + ".";
    }

    private void scrapeAndSaveCode(String code, String userName) {
        CompanyFundamentalDto company = scraperService.scrapeCompany(code);
        companyService.saveScrapedCompany(company, userName);
    }

    private String cleanCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Trading code is required.");
        }
        return code.trim().toUpperCase();
    }

    private String cleanUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            return null;
        }
        return userName.trim();
    }

    public List<TradingCodeRepository.TradingCodeProjection> getAllTradingCodes() {
        return repository.findTradingCodeDetails();
    }
}
