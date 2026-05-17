package com.ihit.stock.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "market_forecast", uniqueConstraints = @UniqueConstraint(name = "uk_forecast_code_base_target", columnNames = {
        "trading_code", "forecast_base_date", "target_date" }))
public class MarketForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trading_code", nullable = false)
    private String tradingCode;

    @Column(name = "forecast_base_date", nullable = false)
    private LocalDate forecastBaseDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "horizon_days", nullable = false)
    private Integer horizonDays;

    @Column(name = "predicted_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal predictedPrice;

    @Column(name = "upper_bound", precision = 19, scale = 4)
    private BigDecimal upperBound;

    @Column(name = "lower_bound", precision = 19, scale = 4)
    private BigDecimal lowerBound;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(name = "actual_price", precision = 19, scale = 4)
    private BigDecimal actualPrice;

    @Column(name = "absolute_error", precision = 19, scale = 4)
    private BigDecimal absoluteError;

    @Column(name = "percentage_error", precision = 19, scale = 4)
    private BigDecimal percentageError;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    public Long getId() {
        return id;
    }

    public String getTradingCode() {
        return tradingCode;
    }

    public void setTradingCode(String tradingCode) {
        this.tradingCode = tradingCode;
    }

    public LocalDate getForecastBaseDate() {
        return forecastBaseDate;
    }

    public void setForecastBaseDate(LocalDate forecastBaseDate) {
        this.forecastBaseDate = forecastBaseDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Integer getHorizonDays() {
        return horizonDays;
    }

    public void setHorizonDays(Integer horizonDays) {
        this.horizonDays = horizonDays;
    }

    public BigDecimal getPredictedPrice() {
        return predictedPrice;
    }

    public void setPredictedPrice(BigDecimal predictedPrice) {
        this.predictedPrice = predictedPrice;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }

    public LocalDate getActualDate() {
        return actualDate;
    }

    public void setActualDate(LocalDate actualDate) {
        this.actualDate = actualDate;
    }

    public BigDecimal getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }

    public BigDecimal getAbsoluteError() {
        return absoluteError;
    }

    public void setAbsoluteError(BigDecimal absoluteError) {
        this.absoluteError = absoluteError;
    }

    public BigDecimal getPercentageError() {
        return percentageError;
    }

    public void setPercentageError(BigDecimal percentageError) {
        this.percentageError = percentageError;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }
}
