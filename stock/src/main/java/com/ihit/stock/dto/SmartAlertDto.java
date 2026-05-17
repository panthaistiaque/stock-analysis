package com.ihit.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SmartAlertDto {

    private final String alertType;
    private final String tradingCode;
    private final String description;
    private final String severity;
    private final LocalDate triggerDate;
    private final BigDecimal currentPrice;
    private final Integer confidencePercent;

    public SmartAlertDto(String alertType, String tradingCode, String description, String severity,
            LocalDate triggerDate, BigDecimal currentPrice, Integer confidencePercent) {
        this.alertType = alertType;
        this.tradingCode = tradingCode;
        this.description = description;
        this.severity = severity;
        this.triggerDate = triggerDate;
        this.currentPrice = currentPrice;
        this.confidencePercent = confidencePercent;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getTradingCode() {
        return tradingCode;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public LocalDate getTriggerDate() {
        return triggerDate;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public Integer getConfidencePercent() {
        return confidencePercent;
    }
}
