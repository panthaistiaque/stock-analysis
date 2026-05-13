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
@Table(name = "stock_market_data", uniqueConstraints = @UniqueConstraint(name = "uk_stock_date_code", columnNames = {
        "trade_date", "trading_code" }))
public class StockMarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate date;

    @Column(name = "trading_code", nullable = false, comment = "trading code of the company")
    private String tradingCode;

    @Column(name = "last_traded_price", nullable = false, comment = "last traded price of the day")
    private BigDecimal ltp;

    @Column(name = "high_price", nullable = false, comment = "high price of the day")
    private BigDecimal high;
    
    @Column(name = "low_price", nullable = false, comment = "low price of the day")
    private BigDecimal low;
    
    @Column(name = "open_price", nullable = false, comment = "open price of the day")
    private BigDecimal openp;
    
    @Column(name = "close_price", nullable = false, comment = "close price of the day")
    private BigDecimal closep;
    
    @Column(name = "yearsterday_close_price", nullable = false, comment = "yearsterday close price")
    private BigDecimal ycp;
    
    @Column(name = "trade_no", nullable = false, comment = "number of trades in a day")
    private Long trade;
    
    @Column(name = "trade_value", nullable = false, comment = "trade value in a day")
    private BigDecimal tradeValue;
    
    @Column(name = "volume", nullable = false, comment = "volume in a day")
    private Long volume;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false, comment = "store creation datetime")
    private LocalDateTime createdOn;

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

    public Long getTrade() {
        return trade;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }


    public void update(LocalDate date, String tradingCode, BigDecimal ltp, BigDecimal high, BigDecimal low,
            BigDecimal openp, BigDecimal closep, BigDecimal ycp, Long trade, BigDecimal tradeValue, Long volume) {
        this.date = date;
        this.tradingCode = tradingCode;
        this.ltp = ltp;
        this.high = high;
        this.low = low;
        this.openp = openp;
        this.closep = closep;
        this.ycp = ycp;
        this.trade = trade;
        this.tradeValue = tradeValue;
        this.volume = volume;
    }
}
