package com.ihit.stock.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class EpsHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String year;
    private BigDecimal eps;
    private BigDecimal navPerShare;
    private BigDecimal profit;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public BigDecimal getEps() { return eps; }
    public void setEps(BigDecimal eps) { this.eps = eps; }
    public BigDecimal getNavPerShare() { return navPerShare; }
    public void setNavPerShare(BigDecimal navPerShare) { this.navPerShare = navPerShare; }
    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }
}
