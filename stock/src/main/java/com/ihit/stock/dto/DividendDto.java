package com.ihit.stock.dto;

public class DividendDto {

    private String year;

    private String cashDividend;
    private String stockDividend;

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getCashDividend() {
        return cashDividend;
    }

    public void setCashDividend(String cashDividend) {
        this.cashDividend = cashDividend;
    }

    public String getStockDividend() {
        return stockDividend;
    }

    public void setStockDividend(String stockDividend) {
        this.stockDividend = stockDividend;
    }
}