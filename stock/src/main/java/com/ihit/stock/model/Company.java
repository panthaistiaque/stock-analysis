package com.ihit.stock.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Company {
    @Id
    private String tradingCode;
    private String companyName;
    private String sector;
    private String marketCategory;
    private BigDecimal lastTradingPrice;
    private BigDecimal marketCapitalization;
    private BigDecimal paidUpCapital;
    private String week52Range;
    private BigDecimal peRatio;
    private BigDecimal dividendYield;
    private Long outstandingSecurities;
    private BigDecimal shortTermLoan;
    private BigDecimal longTermLoan;
    private String sponsorHolding;
    private String govtHolding;
    private String instituteHolding;
    private String foreignHolding;
    private String publicHolding;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scrapingDate;
    private String scrapingUserName;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "company_code")
    private List<EpsHistory> epsHistory = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "company_code")
    private List<DividendHistory> dividends = new ArrayList<>();

    // Getters and Setters
    public String getTradingCode() { return tradingCode; }
    public void setTradingCode(String tradingCode) { this.tradingCode = tradingCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public String getMarketCategory() { return marketCategory; }
    public void setMarketCategory(String marketCategory) { this.marketCategory = marketCategory; }
    public BigDecimal getLastTradingPrice() { return lastTradingPrice; }
    public void setLastTradingPrice(BigDecimal lastTradingPrice) { this.lastTradingPrice = lastTradingPrice; }
    public BigDecimal getMarketCapitalization() { return marketCapitalization; }
    public void setMarketCapitalization(BigDecimal marketCapitalization) { this.marketCapitalization = marketCapitalization; }
    public BigDecimal getPaidUpCapital() { return paidUpCapital; }
    public void setPaidUpCapital(BigDecimal paidUpCapital) { this.paidUpCapital = paidUpCapital; }
    public String getWeek52Range() { return week52Range; }
    public void setWeek52Range(String week52Range) { this.week52Range = week52Range; }
    public BigDecimal getPeRatio() { return peRatio; }
    public void setPeRatio(BigDecimal peRatio) { this.peRatio = peRatio; }
    public BigDecimal getDividendYield() { return dividendYield; }
    public void setDividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; }
    public Long getOutstandingSecurities() { return outstandingSecurities; }
    public void setOutstandingSecurities(Long outstandingSecurities) { this.outstandingSecurities = outstandingSecurities; }
    public BigDecimal getShortTermLoan() { return shortTermLoan; }
    public void setShortTermLoan(BigDecimal shortTermLoan) { this.shortTermLoan = shortTermLoan; }
    public BigDecimal getLongTermLoan() { return longTermLoan; }
    public void setLongTermLoan(BigDecimal longTermLoan) { this.longTermLoan = longTermLoan; }
    public String getSponsorHolding() { return sponsorHolding; }
    public void setSponsorHolding(String sponsorHolding) { this.sponsorHolding = sponsorHolding; }
    public String getGovtHolding() { return govtHolding; }
    public void setGovtHolding(String govtHolding) { this.govtHolding = govtHolding; }
    public String getInstituteHolding() { return instituteHolding; }
    public void setInstituteHolding(String instituteHolding) { this.instituteHolding = instituteHolding; }
    public String getForeignHolding() { return foreignHolding; }
    public void setForeignHolding(String foreignHolding) { this.foreignHolding = foreignHolding; }
    public String getPublicHolding() { return publicHolding; }
    public void setPublicHolding(String publicHolding) { this.publicHolding = publicHolding; }
    public LocalDateTime getScrapingDate() { return scrapingDate; }
    public void setScrapingDate(LocalDateTime scrapingDate) { this.scrapingDate = scrapingDate; }
    public String getScrapingUserName() { return scrapingUserName; }
    public void setScrapingUserName(String scrapingUserName) { this.scrapingUserName = scrapingUserName; }
    public List<EpsHistory> getEpsHistory() { return epsHistory; }
    public void setEpsHistory(List<EpsHistory> epsHistory) { this.epsHistory = epsHistory; }
    public List<DividendHistory> getDividends() { return dividends; }
    public void setDividends(List<DividendHistory> dividends) { this.dividends = dividends; }
}
