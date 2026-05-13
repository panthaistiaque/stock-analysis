package com.ihit.stock.dto;

import java.util.ArrayList;
import java.util.List;

public class FundamentalAnalysisSection {
    private String title;
    private String tone;
    private int score;
    private String summary;
    private List<String> calculations = new ArrayList<>();
    private List<String> reasons = new ArrayList<>();

    public FundamentalAnalysisSection(String title, String tone, int score, String summary) {
        this.title = title;
        this.tone = tone;
        this.score = score;
        this.summary = summary;
    }

    public String getTitle() { return title; }
    public String getTone() { return tone; }
    public int getScore() { return score; }
    public String getSummary() { return summary; }
    public List<String> getCalculations() { return calculations; }
    public List<String> getReasons() { return reasons; }

    public FundamentalAnalysisSection addCalculation(String calculation) {
        calculations.add(calculation);
        return this;
    }

    public FundamentalAnalysisSection addReason(String reason) {
        reasons.add(reason);
        return this;
    }
}
