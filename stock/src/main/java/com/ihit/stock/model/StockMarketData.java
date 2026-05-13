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

@Entity
@Table(name = "stock_market_data",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_date_code", columnNames = {"trade_date", "trading_code"}))
public class StockMarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate date;

    @Column(name = "trading_code", nullable = false)
    private String tradingCode;

    private BigDecimal ltp;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal openp;
    private BigDecimal closep;
    private BigDecimal ycp;
    private BigDecimal tradeValue;
    private Long volume;

    protected StockMarketData() {
    }

    public StockMarketData(LocalDate date, String tradingCode) {
        this.date = date;
        this.tradingCode = tradingCode;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTradingCode() {
        return tradingCode;
    }

    public BigDecimal getLtp() {
        return ltp;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getOpenp() {
        return openp;
    }

    public BigDecimal getClosep() {
        return closep;
    }

    public BigDecimal getYcp() {
        return ycp;
    }

    public BigDecimal getTradeValue() {
        return tradeValue;
    }

    public Long getVolume() {
        return volume;
    }

    public void update(LocalDate date, String tradingCode, BigDecimal ltp, BigDecimal high, BigDecimal low,
                       BigDecimal openp, BigDecimal closep, BigDecimal ycp, BigDecimal tradeValue, Long volume) {
        this.date = date;
        this.tradingCode = tradingCode;
        this.ltp = ltp;
        this.high = high;
        this.low = low;
        this.openp = openp;
        this.closep = closep;
        this.ycp = ycp;
        this.tradeValue = tradeValue;
        this.volume = volume;
    }
}
