package com.ihit.stock.model;

import jakarta.persistence.*;

@Entity
public class DividendHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String year;
    private String cashDividend;
    private String stockDividend;

    public DividendHistory() {}

    public DividendHistory(String year, String cashDividend, String stockDividend) {
        this.year = year;
        this.cashDividend = cashDividend;
        this.stockDividend = stockDividend;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getCashDividend() { return cashDividend; }
    public void setCashDividend(String cashDividend) { this.cashDividend = cashDividend; }

    public String getStockDividend() { return stockDividend; }
    public void setStockDividend(String stockDividend) { this.stockDividend = stockDividend; }
}