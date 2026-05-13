package com.ihit.stock.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StockDataForm implements Serializable {

    private Long id;
    private LocalDate date;
    private String tradingCode;
    private BigDecimal ltp;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal openp;
    private BigDecimal closep;
    private BigDecimal ycp;
    private BigDecimal tradeValue;
    private BigDecimal trade;
    private BigDecimal value;
    private Long volume;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTradingCode() {
        return tradingCode;
    }

    public void setTradingCode(String tradingCode) {
        this.tradingCode = tradingCode;
    }

    public BigDecimal getLtp() {
        return ltp;
    }

    public void setLtp(BigDecimal ltp) {
        this.ltp = ltp;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getOpenp() {
        return openp;
    }

    public void setOpenp(BigDecimal openp) {
        this.openp = openp;
    }

    public BigDecimal getClosep() {
        return closep;
    }

    public void setClosep(BigDecimal closep) {
        this.closep = closep;
    }

    public BigDecimal getYcp() {
        return ycp;
    }

    public void setYcp(BigDecimal ycp) {
        this.ycp = ycp;
    }

    public BigDecimal getTradeValue() {
        return tradeValue;
    }

    public void setTradeValue(BigDecimal tradeValue) {
        this.tradeValue = tradeValue;
    }

    public BigDecimal getTrade() {
        return trade;
    }

    public void setTrade(BigDecimal trade) {
        this.trade = trade;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }
}
