package com.ihit.stock.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FundamentalAnalysisReport {
    private String tradingCode;
    private String companyName;
    private String sector;
    private String marketCategory;
    private BigDecimal lastTradingPrice;
    private BigDecimal peRatio;
    private int overallScore;
    private String overallDecision;
    private String overallTone;
    private List<FundamentalAnalysisSection> sections = new ArrayList<>();

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
    public BigDecimal getPeRatio() { return peRatio; }
    public void setPeRatio(BigDecimal peRatio) { this.peRatio = peRatio; }
    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }
    public String getOverallDecision() { return overallDecision; }
    public void setOverallDecision(String overallDecision) { this.overallDecision = overallDecision; }
    public String getOverallTone() { return overallTone; }
    public void setOverallTone(String overallTone) { this.overallTone = overallTone; }
    public List<FundamentalAnalysisSection> getSections() { return sections; }
    public void setSections(List<FundamentalAnalysisSection> sections) { this.sections = sections; }
}
