package com.ihit.stock.dto;

import java.math.BigDecimal;

public class EpsDto {
    private String year;

    private BigDecimal eps;

    private BigDecimal navPerShare;

    private BigDecimal profit;

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public BigDecimal getEps() {
        return eps;
    }

    public void setEps(BigDecimal eps) {
        this.eps = eps;
    }

    public BigDecimal getNavPerShare() {
        return navPerShare;
    }

    public void setNavPerShare(BigDecimal navPerShare) {
        this.navPerShare = navPerShare;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

}
