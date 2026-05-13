package com.ihit.stock.service;

import com.ihit.stock.dto.CompanyListRow;
import com.ihit.stock.dto.CompanyFundamentalDto;
import com.ihit.stock.dto.DividendDto;
import com.ihit.stock.dto.EpsDto;
import com.ihit.stock.model.Company;
import com.ihit.stock.model.DividendHistory;
import com.ihit.stock.model.EpsHistory;
import com.ihit.stock.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;

@Service
public class CompanyService {
    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Company save(Company company) {
        return save(company, null);
    }

    @Transactional
    public Company save(Company company, String scrapingUserName) {
        if (company.getTradingCode() == null || company.getTradingCode().isBlank()) {
            throw new IllegalArgumentException("Trading code is required before saving company information.");
        }

        company.setTradingCode(company.getTradingCode().trim().toUpperCase());
        if (company.getScrapingDate() == null) {
            company.setScrapingDate(LocalDateTime.now());
        }
        company.setScrapingUserName(cleanUserName(scrapingUserName));
        company.setEpsHistory(cleanEpsHistory(company.getEpsHistory()));
        company.setDividends(cleanDividendHistory(company.getDividends()));

        return repository.save(company);
    }

    @Transactional
    public Company saveScrapedCompany(CompanyFundamentalDto dto, String scrapingUserName) {
        Company company = new Company();
        company.setTradingCode(dto.getTradingCode());
        company.setCompanyName(dto.getCompanyName());
        company.setSector(dto.getSector());
        company.setMarketCategory(dto.getMarketCategory());
        company.setLastTradingPrice(parseBigDecimal(dto.getLastTradingPrice()));
        company.setMarketCapitalization(dto.getMarketCapitalization());
        company.setPaidUpCapital(dto.getPaidUpCapital());
        company.setWeek52Range(dto.getWeek52Range());
        company.setPeRatio(parseBigDecimal(dto.getPeRatio()));
        company.setDividendYield(parseBigDecimal(dto.getDividendYield()));
        company.setOutstandingSecurities(dto.getOutstandingSecurities() != null
                ? dto.getOutstandingSecurities().longValue()
                : null);
        company.setShortTermLoan(dto.getShortTermLoan());
        company.setLongTermLoan(dto.getLongTermLoan());
        company.setSponsorHolding(dto.getSponsorHolding());
        company.setGovtHolding(dto.getGovtHolding());
        company.setInstituteHolding(dto.getInstituteHolding());
        company.setForeignHolding(dto.getForeignHolding());
        company.setPublicHolding(dto.getPublicHolding());
        company.setScrapingDate(dto.getScrapingDate());
        company.setEpsHistory(toEpsHistory(dto.getEpsHistory()));
        company.setDividends(toDividendHistory(dto.getDividends()));
        return save(company, scrapingUserName);
    }

    @Transactional(readOnly = true)
    public List<CompanyListRow> findListRows(String code, String sector, String marketCategory,
            BigDecimal minPeRatio, BigDecimal maxPeRatio) {
        return repository.findAll().stream()
                .filter(company -> containsIgnoreCase(company.getTradingCode(), code))
                .filter(company -> containsIgnoreCase(company.getSector(), sector))
                .filter(company -> containsIgnoreCase(company.getMarketCategory(), marketCategory))
                .filter(company -> isInPeRange(company.getPeRatio(), minPeRatio, maxPeRatio))
                .sorted(Comparator.comparing(Company::getTradingCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toListRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public Company findByTradingCode(String tradingCode) {
        if (tradingCode == null || tradingCode.isBlank()) {
            throw new IllegalArgumentException("Trading code is required.");
        }
        Company company = repository.findById(tradingCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Company data not found for " + tradingCode + "."));
        company.setEpsHistory(cleanEpsHistory(company.getEpsHistory()));
        company.setDividends(cleanDividendHistory(company.getDividends()));
        return company;
    }

    private String cleanUserName(String scrapingUserName) {
        if (scrapingUserName == null || scrapingUserName.isBlank()) {
            return null;
        }
        return scrapingUserName.trim();
    }

    private List<EpsHistory> toEpsHistory(List<EpsDto> epsRows) {
        List<EpsHistory> rows = new ArrayList<>();
        if (epsRows == null) {
            return rows;
        }
        for (EpsDto epsDto : epsRows) {
            EpsHistory row = new EpsHistory();
            row.setYear(epsDto.getYear());
            row.setEps(epsDto.getEps());
            row.setNavPerShare(epsDto.getNavPerShare());
            row.setProfit(epsDto.getProfit());
            rows.add(row);
        }
        return rows;
    }

    private List<DividendHistory> toDividendHistory(List<DividendDto> dividendRows) {
        List<DividendHistory> rows = new ArrayList<>();
        if (dividendRows == null) {
            return rows;
        }
        for (DividendDto dividendDto : dividendRows) {
            rows.add(new DividendHistory(
                    dividendDto.getYear(),
                    dividendDto.getCashDividend(),
                    dividendDto.getStockDividend()));
        }
        return rows;
    }

    private List<EpsHistory> cleanEpsHistory(List<EpsHistory> epsHistory) {
        List<EpsHistory> cleanRows = new ArrayList<>();
        if (epsHistory == null) {
            return cleanRows;
        }

        for (EpsHistory row : epsHistory) {
            if (row == null || isBlank(row.getYear())) {
                continue;
            }
            row.setYear(row.getYear().trim());
            cleanRows.add(row);
        }
        cleanRows.sort(Comparator.comparingInt((EpsHistory row) -> parseYear(row.getYear())).reversed());
        return cleanRows;
    }

    private List<DividendHistory> cleanDividendHistory(List<DividendHistory> dividends) {
        List<DividendHistory> cleanRows = new ArrayList<>();
        if (dividends == null) {
            return cleanRows;
        }

        for (DividendHistory row : dividends) {
            if (row == null || isBlank(row.getYear())) {
                continue;
            }
            row.setYear(row.getYear().trim());
            cleanRows.add(row);
        }
        cleanRows.sort(Comparator.comparingInt((DividendHistory row) -> parseYear(row.getYear())).reversed());
        return cleanRows;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CompanyListRow toListRow(Company company) {
        CompanyListRow row = new CompanyListRow();
        row.setCode(company.getTradingCode());
        row.setMarketCategory(company.getMarketCategory());
        row.setSector(company.getSector());
        row.setPeRatio(company.getPeRatio());

        EpsHistory latestEps = latestEps(company.getEpsHistory());
        if (latestEps != null) {
            row.setLastEpsYear(latestEps.getYear());
            row.setLastEps(latestEps.getEps());
        }

        DividendHistory latestDividend = latestDividend(company.getDividends());
        if (latestDividend != null) {
            row.setLastDividendYear(latestDividend.getYear());
            row.setLastCashDividend(latestDividend.getCashDividend());
            row.setLastStockDividend(latestDividend.getStockDividend());
        }

        return row;
    }

    public EpsHistory latestEps(List<EpsHistory> epsHistory) {
        if (epsHistory == null) {
            return null;
        }
        return epsHistory.stream()
                .filter(row -> row != null && !isBlank(row.getYear()))
                .max(Comparator.comparingInt(row -> parseYear(row.getYear())))
                .orElse(null);
    }

    public DividendHistory latestDividend(List<DividendHistory> dividends) {
        if (dividends == null) {
            return null;
        }
        return dividends.stream()
                .filter(row -> row != null && !isBlank(row.getYear()))
                .max(Comparator.comparingInt(row -> parseYear(row.getYear())))
                .orElse(null);
    }

    private boolean containsIgnoreCase(String value, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase().contains(search.trim().toLowerCase());
    }

    private boolean isInPeRange(BigDecimal peRatio, BigDecimal minPeRatio, BigDecimal maxPeRatio) {
        if (peRatio == null) {
            return minPeRatio == null && maxPeRatio == null;
        }
        if (minPeRatio != null && peRatio.compareTo(minPeRatio) < 0) {
            return false;
        }
        return maxPeRatio == null || peRatio.compareTo(maxPeRatio) <= 0;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace(",", "").replace("%", "").trim();
        if (normalized.isBlank() || "-".equals(normalized) || "N/A".equalsIgnoreCase(normalized)) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    private int parseYear(String year) {
        try {
            return Integer.parseInt(year);
        } catch (Exception e) {
            return 0;
        }
    }
}
