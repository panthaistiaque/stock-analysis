package com.ihit.stock.dto;

import java.math.BigDecimal;

public class CompanyListRow {
    private String code;
    private String marketCategory;
    private String sector;
    private BigDecimal peRatio;
    private String lastEpsYear;
    private BigDecimal lastEps;
    private String lastDividendYear;
    private String lastCashDividend;
    private String lastStockDividend;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMarketCategory() { return marketCategory; }
    public void setMarketCategory(String marketCategory) { this.marketCategory = marketCategory; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public BigDecimal getPeRatio() { return peRatio; }
    public void setPeRatio(BigDecimal peRatio) { this.peRatio = peRatio; }
    public String getLastEpsYear() { return lastEpsYear; }
    public void setLastEpsYear(String lastEpsYear) { this.lastEpsYear = lastEpsYear; }
    public BigDecimal getLastEps() { return lastEps; }
    public void setLastEps(BigDecimal lastEps) { this.lastEps = lastEps; }
    public String getLastDividendYear() { return lastDividendYear; }
    public void setLastDividendYear(String lastDividendYear) { this.lastDividendYear = lastDividendYear; }
    public String getLastCashDividend() { return lastCashDividend; }
    public void setLastCashDividend(String lastCashDividend) { this.lastCashDividend = lastCashDividend; }
    public String getLastStockDividend() { return lastStockDividend; }
    public void setLastStockDividend(String lastStockDividend) { this.lastStockDividend = lastStockDividend; }

    public String getLastEpsDisplay() {
        if (lastEpsYear == null) {
            return "-";
        }
        String epsText = lastEps != null ? lastEps.toPlainString() : "0";
        return epsText + " (" + lastEpsYear + ")";
    }

    public String getLastDividendDisplay() {
        if (lastDividendYear == null) {
            return "-";
        }
        String cashText = isBlank(lastCashDividend) ? "0" : lastCashDividend;
        String stockText = isBlank(lastStockDividend) ? "0" : lastStockDividend;
        return "Cash " + cashText + "%, Stock " + stockText + "% (" + lastDividendYear + ")";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
