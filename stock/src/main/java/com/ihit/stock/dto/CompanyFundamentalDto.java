package com.ihit.stock.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

public class CompanyFundamentalDto {
    private String tradingCode;
    private String companyName;

    private String sector;
    private String marketCategory;

    private String lastTradingPrice;
    private BigDecimal marketCapitalization;
    private BigDecimal paidUpCapital;

    private String week52Range;

    private String peRatio;
    private String dividendYield;

    private BigInteger outstandingSecurities;
    private String sponsorHolding;
    private String govtHolding;
    private String instituteHolding;
    private String foreignHolding;
    private String publicHolding;

    private BigDecimal shortTermLoan;
    private BigDecimal longTermLoan;
    private LocalDateTime scrapingDate;

    private List<DividendDto> dividends;
    private List<EpsDto> epsHistory;

    public List<EpsDto> getEpsHistory() {
        return epsHistory;
    }

    public void setEpsHistory(List<EpsDto> epsHistory) {
        this.epsHistory = epsHistory;
    }

    public String getTradingCode() {
        return tradingCode;
    }

    public void setTradingCode(String tradingCode) {
        this.tradingCode = tradingCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getMarketCategory() {
        return marketCategory;
    }

    public void setMarketCategory(String marketCategory) {
        this.marketCategory = marketCategory;
    }

    public String getLastTradingPrice() {
        return lastTradingPrice;
    }

    public void setLastTradingPrice(String lastTradingPrice) {
        this.lastTradingPrice = lastTradingPrice;
    }

    public BigDecimal getMarketCapitalization() {
        return marketCapitalization;
    }

    public void setMarketCapitalization(BigDecimal marketCapitalization) {
        this.marketCapitalization = marketCapitalization;
    }

    public BigDecimal getPaidUpCapital() {
        return paidUpCapital;
    }

    public void setPaidUpCapital(BigDecimal paidUpCapital) {
        this.paidUpCapital = paidUpCapital;
    }

    public String getWeek52Range() {
        return week52Range;
    }

    public void setWeek52Range(String week52Range) {
        this.week52Range = week52Range;
    }

    public String getPeRatio() {
        return peRatio;
    }

    public void setPeRatio(String peRatio) {
        this.peRatio = peRatio;
    }

    public String getDividendYield() {
        return dividendYield;
    }

    public void setDividendYield(String dividendYield) {
        this.dividendYield = dividendYield;
    }

    public String getSponsorHolding() {
        return sponsorHolding;
    }

    public void setSponsorHolding(String sponsorHolding) {
        this.sponsorHolding = sponsorHolding;
    }

    public String getGovtHolding() {
        return govtHolding;
    }

    public void setGovtHolding(String govtHolding) {
        this.govtHolding = govtHolding;
    }

    public String getInstituteHolding() {
        return instituteHolding;
    }

    public void setInstituteHolding(String instituteHolding) {
        this.instituteHolding = instituteHolding;
    }

    public String getForeignHolding() {
        return foreignHolding;
    }

    public void setForeignHolding(String foreignHolding) {
        this.foreignHolding = foreignHolding;
    }

    public String getPublicHolding() {
        return publicHolding;
    }

    public void setPublicHolding(String publicHolding) {
        this.publicHolding = publicHolding;
    }

    public BigDecimal getShortTermLoan() {
        return shortTermLoan;
    }

    public void setShortTermLoan(BigDecimal shortTermLoan) {
        this.shortTermLoan = shortTermLoan;
    }

    public BigDecimal getLongTermLoan() {
        return longTermLoan;
    }

    public void setLongTermLoan(BigDecimal longTermLoan) {
        this.longTermLoan = longTermLoan;
    }

    public LocalDateTime getScrapingDate() {
        return scrapingDate;
    }

    public void setScrapingDate(LocalDateTime scrapingDate) {
        this.scrapingDate = scrapingDate;
    }

    public List<DividendDto> getDividends() {
        return dividends;
    }

    public void setDividends(List<DividendDto> dividends) {
        this.dividends = dividends;
    }

    public BigInteger getOutstandingSecurities() {
        return outstandingSecurities;
    }

    public void setOutstandingSecurities(BigInteger outstandingSecurities) {
        this.outstandingSecurities = outstandingSecurities;
    }

}
