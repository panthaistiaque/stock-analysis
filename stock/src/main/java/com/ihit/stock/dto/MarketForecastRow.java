package com.ihit.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MarketForecastRow {

    private final String tradingCode;
    private final LocalDate forecastBaseDate;
    private final LocalDate targetDate;
    private final Integer horizonDays;
    private final BigDecimal predictedPrice;
    private final BigDecimal upperBound;
    private final BigDecimal lowerBound;
    private final LocalDate actualDate;
    private final BigDecimal actualPrice;
    private final BigDecimal absoluteError;
    private final BigDecimal percentageError;

    public MarketForecastRow(String tradingCode, LocalDate forecastBaseDate, LocalDate targetDate, Integer horizonDays,
            BigDecimal predictedPrice, BigDecimal upperBound, BigDecimal lowerBound, LocalDate actualDate,
            BigDecimal actualPrice, BigDecimal absoluteError, BigDecimal percentageError) {
        this.tradingCode = tradingCode;
        this.forecastBaseDate = forecastBaseDate;
        this.targetDate = targetDate;
        this.horizonDays = horizonDays;
        this.predictedPrice = predictedPrice;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.actualDate = actualDate;
        this.actualPrice = actualPrice;
        this.absoluteError = absoluteError;
        this.percentageError = percentageError;
    }

    public String getTradingCode() {
        return tradingCode;
    }

    public LocalDate getForecastBaseDate() {
        return forecastBaseDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getTargetDateIso() {
        return targetDate == null ? null : targetDate.toString();
    }

    public Integer getHorizonDays() {
        return horizonDays;
    }

    public BigDecimal getPredictedPrice() {
        return predictedPrice;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public LocalDate getActualDate() {
        return actualDate;
    }

    public BigDecimal getActualPrice() {
        return actualPrice;
    }

    public BigDecimal getAbsoluteError() {
        return absoluteError;
    }

    public BigDecimal getPercentageError() {
        return percentageError;
    }
}
