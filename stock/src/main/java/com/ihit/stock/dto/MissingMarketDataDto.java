package com.ihit.stock.dto;

import java.time.LocalDate;

public class MissingMarketDataDto {
    private String tradingCode;
    private LocalDate lastDate;
    private LocalDate missingFrom;

    public MissingMarketDataDto(String tradingCode, LocalDate lastDate, LocalDate missingFrom) {
        this.tradingCode = tradingCode;
        this.lastDate = lastDate;
        this.missingFrom = missingFrom;
    }

    public String getTradingCode() {
        return tradingCode;
    }

    public void setTradingCode(String tradingCode) {
        this.tradingCode = tradingCode;
    }

    public LocalDate getLastDate() {
        return lastDate;
    }

    public void setLastDate(LocalDate lastDate) {
        this.lastDate = lastDate;
    }

    public LocalDate getMissingFrom() {
        return missingFrom;
    }

    public void setMissingFrom(LocalDate missingFrom) {
        this.missingFrom = missingFrom;
    }
}