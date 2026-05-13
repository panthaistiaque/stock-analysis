package com.ihit.stock.service;

import com.ihit.stock.dto.FundamentalAnalysisReport;
import com.ihit.stock.dto.FundamentalAnalysisSection;
import com.ihit.stock.model.Company;
import com.ihit.stock.model.DividendHistory;
import com.ihit.stock.model.EpsHistory;
import com.ihit.stock.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FundamentalAnalysisService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CompanyRepository repository;

    public FundamentalAnalysisService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public FundamentalAnalysisReport analyze(String tradingCode) {
        Company company = repository.findById(tradingCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + tradingCode));
        List<EpsHistory> epsRows = sortedEps(company.getEpsHistory());
        List<DividendHistory> dividendRows = sortedDividends(company.getDividends());

        FundamentalAnalysisReport report = new FundamentalAnalysisReport();
        report.setTradingCode(company.getTradingCode());
        report.setCompanyName(company.getCompanyName());
        report.setSector(company.getSector());
        report.setMarketCategory(company.getMarketCategory());
        report.setLastTradingPrice(company.getLastTradingPrice());
        report.setPeRatio(company.getPeRatio());

        List<FundamentalAnalysisSection> sections = new ArrayList<>();
        sections.add(epsTrendSection(epsRows));
        sections.add(valuationSection(company, epsRows));
        sections.add(grahamValuationSection(company, epsRows));
        sections.add(dividendSection(company, dividendRows, epsRows));
        sections.add(debtSection(company, epsRows));
        sections.add(ownershipSection(company));
        sections.add(historicalTrendSection(company, epsRows, dividendRows));
        sections.add(sizeSection(company));

        report.setSections(sections);
        int score = (int) Math.round(sections.stream().mapToInt(FundamentalAnalysisSection::getScore).average().orElse(0));
        report.setOverallScore(score);
        report.setOverallTone(tone(score));
        report.setOverallDecision(decision(score));
        return report;
    }

    private FundamentalAnalysisSection historicalTrendSection(Company company, List<EpsHistory> epsRows, List<DividendHistory> dividendRows) {
        // --- 1. EPS TREND ANALYSIS (45%) ---
        double earningsScore = 0;
        String epsLabel = "No Trend Data";
        int totalEpsYears = epsRows.size();
        BigDecimal wma = BigDecimal.ZERO;

        if (!epsRows.isEmpty()) {
            BigDecimal latest = nz(epsRows.get(0).getEps());
            BigDecimal oldest = nz(epsRows.get(epsRows.size() - 1).getEps());
            long positiveYears = epsRows.stream().filter(r -> nz(r.getEps()).compareTo(BigDecimal.ZERO) > 0).count();
            double consistency = (double) positiveYears / totalEpsYears;
            
            // 3-Year Weighted Moving Average (WMA) for Smoothed Trend
            if (totalEpsYears >= 3) {
                wma = nz(epsRows.get(0).getEps()).multiply(new BigDecimal("3"))
                        .add(nz(epsRows.get(1).getEps()).multiply(new BigDecimal("2")))
                        .add(nz(epsRows.get(2).getEps()).multiply(BigDecimal.ONE))
                        .divide(new BigDecimal("6"), 2, RoundingMode.HALF_UP);
            }

            if (latest.compareTo(oldest) > 0 && latest.compareTo(BigDecimal.ZERO) > 0) {
                epsLabel = (oldest.compareTo(BigDecimal.ZERO) <= 0) ? "Improving Recovery Trend" : "Strong Growth Trend";
                earningsScore = 100;
            } else if (latest.compareTo(oldest) >= 0) {
                epsLabel = "Stable Earnings Trend";
                earningsScore = 75;
            } else if (latest.compareTo(BigDecimal.ZERO) <= 0 && latest.compareTo(oldest) < 0) {
                epsLabel = "Weakening Earnings Trend";
                earningsScore = 20;
            } else {
                epsLabel = "Volatile Earnings Pattern";
                earningsScore = 45;
            }
            earningsScore = (earningsScore * 0.7) + (consistency * 30);
        }

        // --- 2. DIVIDEND TREND ANALYSIS (30%) ---
        double divScore = 0;
        String divLabel = "No Recent Dividend Support";
        int checkYears = Math.min(5, totalEpsYears > 0 ? totalEpsYears : 5);
        long cashPaid = dividendRows.stream().limit(checkYears)
                .filter(d -> parsePercent(d.getCashDividend()).compareTo(BigDecimal.ZERO) > 0).count();
        
        if (cashPaid >= 4) {
            divLabel = "Stable Dividend Trend";
            divScore = 100;
        } else if (cashPaid >= 2) {
            divLabel = "Inconsistent Dividend Pattern";
            divScore = 60;
        } else if (dividendRows.size() > 0) {
            divLabel = "Declining Shareholder Return";
            divScore = 25;
        }
        
        // Dilution Alert (Stock Dividend Dependency)
        long stockOnlyYears = dividendRows.stream().limit(checkYears)
                .filter(d -> parsePercent(d.getStockDividend()).compareTo(BigDecimal.ZERO) > 0 
                          && parsePercent(d.getCashDividend()).compareTo(BigDecimal.ZERO) == 0).count();

        // --- 3. NAV TREND ANALYSIS (25%) ---
        double navScore = 50;
        String navLabel = "Stable NAV Trend";
        if (epsRows.size() >= 2) {
            BigDecimal latestNav = nz(epsRows.get(0).getNavPerShare());
            BigDecimal oldestNav = nz(epsRows.get(epsRows.size() - 1).getNavPerShare());
            if (latestNav.compareTo(oldestNav) > 0) {
                navLabel = "Strengthening Asset Trend";
                navScore = 100;
            } else if (latestNav.compareTo(oldestNav) < 0) {
                navLabel = "Weakening Asset Base";
                navScore = 20;
            }
        }

        // Weighted Total
        int finalScore = (int) ((earningsScore * 0.45) + (divScore * 0.30) + (navScore * 0.25));

        String overallLabel;
        if (finalScore >= 80) overallLabel = "Strong Historical Trend";
        else if (finalScore >= 65) overallLabel = "Stable Historical Trend";
        else if (finalScore >= 45) overallLabel = "Mixed Trend Pattern";
        else if (finalScore >= 30) overallLabel = "Weak Historical Trend";
        else overallLabel = "Deteriorating Historical Trend";

        FundamentalAnalysisSection section = section("Historical Trend Analysis", tone(finalScore), finalScore, overallLabel);

        if (wma.compareTo(BigDecimal.ZERO) > 0) {
            section.addCalculation("Smoothed EPS (3Y WMA): " + format(wma));
        }
        if (stockOnlyYears >= 2) {
            section.addReason("Flag: High reliance on stock dividends (Dilution Risk).");
        }

        // Calculations/Metadata for UI
        section.addCalculation("EPS Trend: " + epsLabel);
        section.addCalculation("Dividend Track: " + divLabel + " (" + cashPaid + "/" + checkYears + " years)");
        section.addCalculation("Asset Trend: " + navLabel);

        // Dynamic Reasoning
        if (finalScore >= 80) {
            section.addReason("Historical performance shows stable profitability and supportive NAV growth.");
        } else if (finalScore >= 65) {
            section.addReason("The company maintains a reliable operational track record despite minor cyclical fluctuations.");
        } else if (epsLabel.contains("Recovery")) {
            section.addReason("Loss trends have improved gradually, though profitability recovery remains incomplete.");
        } else if (finalScore >= 45) {
            section.addReason("Historical trends remain mixed due to inconsistent dividend continuity or earnings volatility.");
        } else {
            section.addReason("Weakening earnings and declining shareholder return reduce long-term trend quality.");
        }

        // Trend Flags
        if (epsLabel.equals("Strong Growth Trend")) section.addReason("Flag: Stable earnings growth detected.");
        if (epsLabel.equals("Volatile Earnings Pattern")) section.addReason("Flag: Earnings volatility elevated.");
        if (cashPaid < 2 && checkYears > 0) section.addReason("Flag: Dividend continuity weak.");
        if (navLabel.equals("Strengthening Asset Trend")) section.addReason("Flag: NAV trend improving.");
        if (divLabel.equals("Declining Shareholder Return")) section.addReason("Flag: Shareholder return declining.");
        if (epsLabel.equals("Improving Recovery Trend")) section.addReason("Flag: Recovery trend detected.");

        return section;
    }

    private FundamentalAnalysisSection epsTrendSection(List<EpsHistory> epsRows) {
        if (epsRows.isEmpty()) {
            return section("EPS Trend & Quality", "neutral", 40, "EPS history is not available.")
                    .addReason("No EPS rows were found, so growth quality cannot be judged.");
        }

        int totalYears = epsRows.size();
        EpsHistory latest = epsRows.get(0);
        EpsHistory oldest = epsRows.get(epsRows.size() - 1);
        BigDecimal latestEps = nz(latest.getEps());
        BigDecimal oldestEps = nz(oldest.getEps());
        BigDecimal growth = percentChange(latestEps, oldestEps);
        BigDecimal avgEps = averageEps(epsRows);
        
        int positiveYears = (int) epsRows.stream()
                .filter(row -> nz(row.getEps()).compareTo(BigDecimal.ZERO) > 0)
                .count();

        // 1. Profitability Status Classification
        String label;
        double statusScore; // Weight 40%
        if (latestEps.compareTo(BigDecimal.ZERO) > 0 && oldestEps.compareTo(BigDecimal.ZERO) > 0) {
            label = growth.compareTo(BigDecimal.ZERO) >= 0 ? "Strong Earnings Growth" : "Stable Profitability";
            statusScore = 100;
        } else if (latestEps.compareTo(BigDecimal.ZERO) > 0 && oldestEps.compareTo(BigDecimal.ZERO) <= 0) {
            label = "Turnaround Recovery";
            statusScore = 80;
        } else if (latestEps.compareTo(BigDecimal.ZERO) <= 0 && latestEps.compareTo(oldestEps) > 0) {
            label = "Improving Loss Trend";
            statusScore = 30;
        } else if (latestEps.compareTo(BigDecimal.ZERO) <= 0 && latestEps.compareTo(oldestEps) <= 0) {
            label = "Weakening Earnings Trend";
            statusScore = 10;
        } else {
            label = "Volatile Earnings";
            statusScore = 50;
        }

        // 2. Growth Trend Analysis (Weight 30%)
        double trendScore;
        if (growth.compareTo(new BigDecimal("20")) >= 0) trendScore = 100;
        else if (growth.compareTo(BigDecimal.ZERO) >= 0) trendScore = 70;
        else if (growth.compareTo(new BigDecimal("-20")) >= 0) trendScore = 40;
        else trendScore = 10;

        // 3. EPS Stability / Consistency (Weight 30%: 20% Stability + 10% Consistency)
        double consistencyScore = ((double) positiveYears / totalYears) * 100;
        
        // Count sign changes to check volatility
        int signChanges = 0;
        for (int i = 1; i < epsRows.size(); i++) {
            if (nz(epsRows.get(i).getEps()).signum() != nz(epsRows.get(i-1).getEps()).signum()) {
                signChanges++;
            }
        }
        double stabilityScore = Math.max(0, 100 - (signChanges * 33));

        // Final Weighted Scoring
        int finalScore = (int) (
            (statusScore * 0.40) + 
            (trendScore * 0.30) + 
            (stabilityScore * 0.20) + 
            (consistencyScore * 0.10)
        );

        // 4. SMART SCORE LIMITING: If no positive years, cap at 35.
        if (positiveYears == 0) {
            finalScore = Math.min(finalScore, 35);
        }

        FundamentalAnalysisSection section = section("EPS Trend & Quality", tone(finalScore), finalScore, label);
        
        // Calculations Block
        section.addCalculation("Latest EPS: " + format(latestEps) + " (" + latest.getYear() + ")");
        section.addCalculation("Oldest EPS: " + format(oldestEps) + " (" + oldest.getYear() + ")");
        section.addCalculation("Formula: Relative Change = ((Latest - Oldest) / |Oldest|) × 100");
        section.addCalculation("Relative Change: ((" + format(latestEps) + " - " + format(oldestEps) + ") / " + format(oldestEps.abs()) + ") × 100 = " + format(growth) + "%");
        section.addCalculation("Profitability Consistency = " + positiveYears + " / " + totalYears + " positive years");
        section.addCalculation("Average EPS (Annualized) = " + format(avgEps));

        // Dynamic Reasoning Logic
        if (positiveYears == totalYears) {
            section.addReason("Consistent positive EPS supports strong earnings quality and fundamental reliability.");
        } else if (positiveYears == 0) {
            section.addReason("CRITICAL: Company has failed to achieve profitability in any of the recorded periods.");
            if (latestEps.compareTo(oldestEps) > 0) {
                section.addReason("Note: While losses are narrowing, the business model remains structurally loss-making.");
            }
        } else {
            section.addReason("Earnings remain volatile with alternating profit and loss periods, indicating low earnings predictability.");
        }

        if (latestEps.compareTo(BigDecimal.ZERO) > 0 && growth.compareTo(new BigDecimal("15")) > 0) {
            section.addReason("Strong double-digit growth in earnings provides a solid catalyst for valuation expansion.");
        } else if (latestEps.compareTo(BigDecimal.ZERO) < 0 && latestEps.compareTo(oldestEps) < 0) {
            section.addReason("WARNING: Worsening loss trend detected. Financial sustainability is under significant pressure.");
        }

        // Risk Flags
        if (signChanges > 1) {
            section.addReason("Risk Flag: Elevated earnings volatility detected across the analyzed timeframe.");
        }
        if (latestEps.compareTo(BigDecimal.ZERO) > 0 && avgEps.compareTo(BigDecimal.ZERO) < 0) {
            section.addReason("Caution: Latest profit is a recovery from a negative historical average; long-term stability is not yet proven.");
        }

        return section;
    }

    private FundamentalAnalysisSection valuationSection(Company company, List<EpsHistory> epsRows) {
        BigDecimal pe = nz(company.getPeRatio());
        BigDecimal ltp = company.getLastTradingPrice();

        if (epsRows.isEmpty() || pe.compareTo(BigDecimal.ZERO) <= 0) {
            return section("PE-Based Valuation", "neutral", 40, "Valuation context unavailable")
                    .addReason("Incomplete earnings history or negative PE prevents reliable valuation modeling.");
        }

        // 1. PE Range Score (50% Weight)
        double peScore;
        if (pe.compareTo(new BigDecimal("5")) < 0) peScore = 40; // DSE Distress Zone
        else if (pe.compareTo(new BigDecimal("10")) <= 0) peScore = 100; // Attractive
        else if (pe.compareTo(new BigDecimal("15")) <= 0) peScore = 85;  // Reasonable
        else if (pe.compareTo(new BigDecimal("25")) <= 0) peScore = 60;  // Fair
        else if (pe.compareTo(new BigDecimal("40")) <= 0) peScore = 30;  // Expensive
        else peScore = 10; // Speculative

        // 2. EPS Growth Context (30% Weight)
        BigDecimal latestEps = nz(epsRows.get(0).getEps());
        BigDecimal oldestEps = nz(epsRows.get(epsRows.size() - 1).getEps());
        BigDecimal growth = percentChange(latestEps, oldestEps);
        
        double growthScore;
        if (growth.compareTo(new BigDecimal("15")) >= 0) growthScore = 100;
        else if (growth.compareTo(new BigDecimal("5")) >= 0) growthScore = 80;
        else if (growth.compareTo(BigDecimal.ZERO) >= 0) growthScore = 50;
        else growthScore = 10; // Negative growth penalty

        // 3. EPS Stability Score (20% Weight)
        long positiveYears = epsRows.stream().filter(r -> nz(r.getEps()).compareTo(BigDecimal.ZERO) > 0).count();
        double stabilityScore = ((double) positiveYears / epsRows.size()) * 100;

        // Final Weighted Score
        int finalScore = (int) ((peScore * 0.50) + (growthScore * 0.30) + (stabilityScore * 0.20));

        // Dynamic Labeling
        String summaryLabel;
        if (finalScore >= 80) summaryLabel = "Attractive Valuation";
        else if (finalScore >= 65) summaryLabel = "Moderately Attractive";
        else if (finalScore >= 45) summaryLabel = "Fairly Valued";
        else if (finalScore >= 30) summaryLabel = "Expensive";
        else summaryLabel = "Highly Speculative";

        FundamentalAnalysisSection section = section("PE-Based Valuation", tone(finalScore), finalScore, summaryLabel);
        
        // Calculations Block
        BigDecimal calculatedPe = (latestEps.compareTo(BigDecimal.ZERO) > 0 && ltp != null)
                ? ltp.divide(latestEps, 2, RoundingMode.HALF_UP) : null;

        section.addCalculation("Formula: PE Ratio = Last Trading Price (LTP) / Latest EPS")
               .addCalculation("Derivation: " + value(ltp) + " / " + format(latestEps) + " = " + (calculatedPe != null ? format(calculatedPe) : "N/A"))
               .addCalculation("Trailing PE: " + format(pe))
               .addCalculation("Multi-year EPS Growth: " + format(growth) + "%")
               .addCalculation("Earnings Consistency: " + positiveYears + "/" + epsRows.size() + " positive years");

        // Dynamic Reasoning Engine
        boolean isLowPe = pe.compareTo(new BigDecimal("12")) < 0;
        boolean isHighPe = pe.compareTo(new BigDecimal("25")) > 0;
        boolean isFallingEps = growth.compareTo(BigDecimal.ZERO) < 0;
        boolean isStrongGrowth = growth.compareTo(new BigDecimal("15")) > 0;

        if (isLowPe && isFallingEps) {
            section.addReason("WARNING: Low PE coupled with declining earnings suggests a potential Value Trap.");
            section.addReason("The market may be pricing in further deterioration of the business model.");
        } else if (isLowPe && !isFallingEps) {
            section.addReason("Current PE remains within a relatively attractive range while earnings stability supports valuation.");
            section.addReason("The stock appears fundamentally undervalued relative to its historical earning power.");
        } else if (isHighPe && isStrongGrowth) {
            section.addReason("High valuation is partially justified by robust earnings growth momentum.");
            section.addReason("Investors are paying a premium for growth, but sustainability is key to maintaining this level.");
        } else if (isHighPe && !isStrongGrowth) {
            section.addReason("Current valuation appears elevated and speculative relative to underlying earnings performance.");
            section.addReason("CRITICAL: Significant risk of price correction if growth expectations are not met.");
        } else if (pe.compareTo(new BigDecimal("5")) < 0) {
            section.addReason("Extremely low PE detected. This often indicates financial distress or one-off accounting items in the DSE market.");
        } else {
            section.addReason("PE remains within a moderate range, reflecting a balanced risk-reward profile.");
            section.addReason("Valuation is stable, though it lacks strong growth catalysts to drive a major re-rating.");
        }

        if (calculatedPe != null && pe.subtract(calculatedPe).abs().compareTo(new BigDecimal("2")) > 0) {
            section.addCalculation("Data Note: Stored PE (" + format(pe) + ") differs from Calculated PE (" + format(calculatedPe) + "). Check for recent EPS updates.");
        }

        return section;
    }

    private FundamentalAnalysisSection grahamValuationSection(Company company, List<EpsHistory> epsRows) {
        String methodName = "Intrinsic Value (Graham)";
        BigDecimal ltp = company.getLastTradingPrice();
        
        if (epsRows.isEmpty()) {
            return section(methodName, "neutral", 40, "EPS history is not available.")
                    .addReason("Over/Under valuation cannot be calculated without earnings history.");
        }

        EpsHistory latest = epsRows.get(0);
        BigDecimal latestEps = nz(latest.getEps());
        BigDecimal nav = nz(latest.getNavPerShare());

        if (latestEps.compareTo(BigDecimal.ZERO) <= 0) {
            int score = (ltp != null && ltp.compareTo(nav) <= 0) ? 75 : 30;
            return section("Intrinsic Value (Asset-Based)", tone(score), score, "Negative or zero EPS detected.")
                    .addCalculation("Formula: V = NAV (Book Value)")
                    .addCalculation("Latest NAV: " + format(nav))
                    .addCalculation("Last Trading Price (LTP): " + value(ltp))
                    .addReason("For loss-making companies, intrinsic value is estimated using Net Asset Value (NAV) as the floor.")
                    .addReason(score >= 70 ? "Stock is trading at or below its recorded asset value." : "Market price is significantly higher than net assets despite current losses.");
        }

        if (epsRows.size() < 2) {
            BigDecimal intrinsicValue = latestEps.multiply(new BigDecimal("8.5")).setScale(2, RoundingMode.HALF_UP);
            int score = calculateValuationScore(ltp, intrinsicValue);
            return section(methodName, tone(score), score, "Insufficient history for growth calculation.")
                    .addCalculation("Formula: V = EPS × 8.5 (Base) = " + format(latestEps) + " × 8.5")
                    .addCalculation("V = EPS × 8.5 (Base) = " + format(intrinsicValue))
                    .addReason("Used a standard base multiple of 8.5 due to lack of historical growth data.");
        }

        EpsHistory oldest = epsRows.get(epsRows.size() - 1);
        BigDecimal oldestEps = nz(oldest.getEps());
        int yearSpan = Math.max(1, parseYear(latest.getYear()) - parseYear(oldest.getYear()));
        BigDecimal totalGrowthPct = percentChange(latestEps, oldestEps);
        BigDecimal g = totalGrowthPct.divide(new BigDecimal(yearSpan), 2, RoundingMode.HALF_UP);

        BigDecimal intrinsicValue;
        String strategyDescription;

        if (g.compareTo(BigDecimal.ZERO) > 0) {
            // Standard Graham for growth companies
            BigDecimal multiplier = new BigDecimal("8.5").add(new BigDecimal("2").multiply(g));
            intrinsicValue = latestEps.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            strategyDescription = "Positive growth detected. Applied Graham's formula: V = EPS × (8.5 + 2g).";
        } else {
            // Conservative PE for flat/negative growth
            methodName = "Intrinsic Value (Adjusted PE)";
            intrinsicValue = latestEps.multiply(new BigDecimal("8.0")).setScale(2, RoundingMode.HALF_UP);
            strategyDescription = "Negative growth detected (" + format(g) + "%). Growth adjusted to conservative baseline (0%) using 8x PE multiple.";
        }

        int score = calculateValuationScore(ltp, intrinsicValue);

        FundamentalAnalysisSection resultSection = section(methodName, tone(score), score,
                score >= 70 ? "Stock appears undervalued (Margin of Safety)." : "Stock appears overvalued relative to conservative valuation.")
                .addCalculation("Latest EPS: " + format(latestEps) + " (" + latest.getYear() + ")")
                .addCalculation("Oldest EPS: " + format(oldestEps) + " (" + oldest.getYear() + ")")
                .addCalculation("Year Span: " + yearSpan + " years")
                .addCalculation("Formula: g = [((Latest EPS - Oldest EPS) / |Oldest EPS|) / Year Span] × 100")
                .addCalculation("Annual Growth (g): " + format(g) + "%");

        if (g.compareTo(BigDecimal.ZERO) > 0) {
            resultSection.addCalculation("Formula: V = EPS × (8.5 + 2g) = " + format(latestEps) + " × (8.5 + 2 × " + format(g) + ")");
        } else {
            resultSection.addCalculation("Formula: V = EPS × 8.0 (PE Multiple) = " + format(latestEps) + " × 8.0");
        }

        return resultSection.addCalculation("Calculated Intrinsic Value: " + format(intrinsicValue))
                .addCalculation("Last Trading Price (LTP): " + value(ltp))
                .addReason(strategyDescription)
                .addReason(score >= 70 ? "Market price is currently below the intrinsic value estimate." : "Market price is trading at a premium to the calculated intrinsic value.");
    }

    private int calculateValuationScore(BigDecimal ltp, BigDecimal intrinsicValue) {
        if (ltp == null || ltp.compareTo(BigDecimal.ZERO) <= 0 || intrinsicValue == null || intrinsicValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 50;
        }
        BigDecimal ratio = ltp.divide(intrinsicValue, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.7")) <= 0) return 90; // 30% Margin of safety
        if (ratio.compareTo(BigDecimal.ONE) <= 0) return 75; // Fairly valued
        if (ratio.compareTo(new BigDecimal("1.3")) <= 0) return 50; // Slightly overvalued
        return 30; // Overvalued
    }

    private FundamentalAnalysisSection dividendSection(Company company, List<DividendHistory> dividendRows, List<EpsHistory> epsRows) {
        if (dividendRows.isEmpty()) {
            return section("Dividend Sustainability", "bad", 10, "Non-Dividend Payer")
                    .addReason("No dividend history found. Investor returns rely solely on capital appreciation.");
        }

        int currentYear = LocalDate.now().getYear();
        DividendHistory latest = dividendRows.get(0);
        int latestYear = parseYear(latest.getYear());
        int gapYears = Math.max(0, currentYear - latestYear - 1);

        BigDecimal latestCash = parsePercent(latest.getCashDividend());
        BigDecimal latestStock = parsePercent(latest.getStockDividend());
        BigDecimal ltp = nz(company.getLastTradingPrice());
        BigDecimal faceValue = new BigDecimal("10");
        BigDecimal cashAmt = latestCash.multiply(faceValue).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal yield = (ltp.compareTo(BigDecimal.ZERO) > 0) ? cashAmt.multiply(HUNDRED).divide(ltp, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // 1. Recency Score (40%)
        double recencyScore = gapYears == 0 ? 100 : gapYears == 1 ? 60 : gapYears == 2 ? 20 : 0;

        // 2. Cash Quality (25%)
        double cashQuality = (latestCash.compareTo(BigDecimal.ZERO) > 0 && latestStock.compareTo(BigDecimal.ZERO) == 0) ? 100
                           : (latestCash.compareTo(BigDecimal.ZERO) > 0) ? 70
                           : (latestStock.compareTo(BigDecimal.ZERO) > 0) ? 20 : 0;

        // 3. Continuity Score (20%) - Last 5 years
        long paidInLast5 = dividendRows.stream().limit(5).filter(d -> parsePercent(d.getCashDividend()).compareTo(BigDecimal.ZERO) > 0).count();
        double continuityScore = (paidInLast5 / 5.0) * 100;

        // 4. Yield Strength (15%)
        double yieldScore = yield.compareTo(new BigDecimal("8")) >= 0 ? 100
                          : yield.compareTo(new BigDecimal("4")) >= 0 ? 75
                          : yield.compareTo(new BigDecimal("1")) >= 0 ? 40 : 10;

        // Final Weighted Calculation
        int finalScore = (int) ((recencyScore * 0.40) + (cashQuality * 0.25) + (continuityScore * 0.20) + (yieldScore * 0.15));

        // Hard Penalty for Stale Records (3+ years no dividend)
        if (gapYears >= 3) finalScore = Math.min(finalScore, 20);

        // Logic for Professional Summary & Main Reason
        String summary;
        String mainReason;
        if (gapYears >= 3) {
            summary = "Non-Operational Dividend Track";
            mainReason = "CRITICAL: No dividend declared since " + latest.getYear() + ". This stock is currently a non-income generating asset.";
        } else if (latestCash.compareTo(BigDecimal.ZERO) == 0 && latestStock.compareTo(BigDecimal.ZERO) > 0) {
            summary = "Dilution Risk";
            mainReason = "The company is only issuing bonus shares. This increases share supply (dilution) without providing actual cash flow to investors.";
        } else if (finalScore >= 75) {
            summary = "High Quality Income Gem";
            mainReason = "Consistent cash payout with attractive yield and regular track record.";
        } else {
            summary = "Average Dividend Payer";
            mainReason = "The company maintains a dividend presence but lacks the yield or consistency of top-tier performers.";
        }

        FundamentalAnalysisSection section = section("Dividend Sustainability", tone(finalScore), finalScore, summary);
        
        section.addCalculation("Last Dividend: " + latest.getYear() + " (Recency: " + (gapYears == 0 ? "Current" : gapYears + "y gap") + ")")
               .addCalculation("Type: " + (latestCash.compareTo(BigDecimal.ZERO) > 0 ? "Cash " + latestCash + "%" : "") + (latestStock.compareTo(BigDecimal.ZERO) > 0 ? " Stock " + latestStock + "%" : ""))
               .addCalculation("Formula: Yield = (Cash % × Face Value 10) / LTP")
               .addCalculation("Current Yield: " + yield + "% (Based on LTP " + ltp + ")")
               .addCalculation("Continuity: " + paidInLast5 + " payments in last 5 years")
               .addReason(mainReason);

        // Sustainability Check (Payout vs EPS)
        if (!epsRows.isEmpty() && latestCash.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal latestEps = nz(epsRows.get(0).getEps());
            if (latestEps.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal payoutRatio = cashAmt.multiply(HUNDRED).divide(latestEps, 2, RoundingMode.HALF_UP);
                section.addCalculation("Payout Ratio: " + payoutRatio + "%");
                if (payoutRatio.compareTo(HUNDRED) > 0) {
                    section.addReason("WARNING: Payout ratio exceeds 100%. The company is paying more than it earned, which is unsustainable.");
                }
            }
        }

        return section;
    }

    private FundamentalAnalysisSection debtSection(Company company, List<EpsHistory> epsRows) {
        BigDecimal stLoan = nz(company.getShortTermLoan());
        BigDecimal ltLoan = nz(company.getLongTermLoan());
        BigDecimal totalDebt = stLoan.add(ltLoan);
        BigDecimal marketCap = nz(company.getMarketCapitalization());

        if (marketCap.compareTo(BigDecimal.ZERO) <= 0 && totalDebt.compareTo(BigDecimal.ZERO) <= 0) {
            return section("Financial Leverage & Debt Risk", "neutral", 50, "Debt Data Unavailable")
                    .addReason("Insufficient loan or market capitalization data to assess financial leverage.");
        }

        // 1. Debt-to-Market-Cap Score (50% Weight)
        BigDecimal dtmc = percent(totalDebt, marketCap);
        double dtmcScore;
        if (dtmc == null || dtmc.compareTo(new BigDecimal("30")) < 0) dtmcScore = 100;
        else if (dtmc.compareTo(new BigDecimal("70")) <= 0) dtmcScore = 75;
        else if (dtmc.compareTo(new BigDecimal("120")) <= 0) dtmcScore = 40;
        else dtmcScore = 10;

        // 2. Short-Term Debt Pressure Score (30% Weight)
        BigDecimal stRatio = percent(stLoan, totalDebt);
        double stScore;
        if (totalDebt.compareTo(BigDecimal.ZERO) == 0) stScore = 100;
        else if (stRatio.compareTo(new BigDecimal("30")) < 0) stScore = 100;
        else if (stRatio.compareTo(new BigDecimal("60")) <= 0) stScore = 60;
        else stScore = 20;

        // 3. EPS Support Analysis (20% Weight)
        double epsSupportScore = 50;
        boolean epsImproving = false;
        if (!epsRows.isEmpty()) {
            BigDecimal latestEps = nz(epsRows.get(0).getEps());
            BigDecimal avgEps = averageEps(epsRows);
            long positiveYears = epsRows.stream().filter(r -> nz(r.getEps()).compareTo(BigDecimal.ZERO) > 0).count();
            
            if (latestEps.compareTo(avgEps) >= 0 && positiveYears >= epsRows.size() / 2) {
                epsSupportScore = 100;
                epsImproving = true;
            } else if (latestEps.compareTo(BigDecimal.ZERO) <= 0) {
                epsSupportScore = 10;
            }
        }

        int finalScore = (int) ((dtmcScore * 0.50) + (stScore * 0.30) + (epsSupportScore * 0.20));

        // Recommended Labels
        String statusLabel;
        if (finalScore >= 80) statusLabel = "Low Debt Risk";
        else if (finalScore >= 65) statusLabel = "Manageable Debt";
        else if (finalScore >= 45) statusLabel = "Moderate Leverage";
        else if (finalScore >= 30) statusLabel = "Elevated Debt Risk";
        else statusLabel = "High Financial Pressure";

        FundamentalAnalysisSection section = section("Financial Leverage & Debt Risk", tone(finalScore), finalScore, statusLabel);

        // Calculations Block
        section.addCalculation("Total Debt = ST Loan + LT Loan = " + format(totalDebt) )
               .addCalculation("Debt-to-Market-Cap = (Total Debt / Market Cap) x 100 = " + percentText(dtmc))
               .addCalculation("Short-Term Debt Ratio = (ST Loan / Total Debt) x 100 = " + (totalDebt.compareTo(BigDecimal.ZERO) > 0 ? percentText(stRatio) : "0%"));

        // Dynamic Reasoning & Risk Flags
        if (dtmc != null && dtmc.compareTo(new BigDecimal("100")) > 0) {
            section.addReason("CRITICAL: Total debt exceeds the company's market capitalization, indicating extreme leverage.");
        } else if (dtmc != null && dtmc.compareTo(new BigDecimal("70")) > 0) {
            section.addReason("Debt levels appear elevated relative to company size.");
        } else {
            section.addReason("Debt burden remains manageable relative to company size.");
        }

        if (stRatio != null && stRatio.compareTo(new BigDecimal("60")) > 0 && totalDebt.compareTo(BigDecimal.ZERO) > 0) {
            section.addReason("WARNING: Higher short-term loan exposure may increase immediate refinancing and liquidity pressure.");
        } else if (totalDebt.compareTo(BigDecimal.ZERO) > 0 && stRatio.compareTo(new BigDecimal("30")) < 0) {
            section.addReason("Predominantly long-term debt structure reduces immediate liquidity risk.");
        }

        if (!epsRows.isEmpty()) {
            if (!epsImproving && dtmc != null && dtmc.compareTo(new BigDecimal("50")) > 0) {
                section.addReason("Negative or stagnant earnings trend reduces the margin of safety for debt servicing.");
            } else if (epsImproving) {
                section.addReason("Current earnings stability provides supportive coverage for existing leverage.");
            }
        }

        return section;
    }

    private FundamentalAnalysisSection ownershipSection(Company company) {
        BigDecimal sponsor = parsePercent(company.getSponsorHolding());
        BigDecimal institute = parsePercent(company.getInstituteHolding());
        BigDecimal foreign = parsePercent(company.getForeignHolding());
        BigDecimal publicHolding = parsePercent(company.getPublicHolding());

        // 1. Sponsor Holding Score (45%)
        double sponsorScore;
        if (sponsor.compareTo(new BigDecimal("85")) > 0) sponsorScore = 50; // Liquidity Trap
        else if (sponsor.compareTo(new BigDecimal("50")) >= 0) sponsorScore = 100; // Strong Confidence
        else if (sponsor.compareTo(new BigDecimal("30")) >= 0) sponsorScore = 80; // Healthy / Regulatory Min
        else if (sponsor.compareTo(new BigDecimal("20")) >= 0) sponsorScore = 40; // Low Confidence
        else sponsorScore = 10; // Critical Risk

        // 2. Institutional Support Score (30%)
        double instScore;
        if (institute.compareTo(new BigDecimal("20")) >= 0) instScore = 100;
        else if (institute.compareTo(new BigDecimal("10")) >= 0) instScore = 70;
        else if (institute.compareTo(new BigDecimal("5")) >= 0) instScore = 40;
        else instScore = 20;

        // 3. Foreign Participation Score (10%)
        double foreignScore;
        if (foreign.compareTo(new BigDecimal("10")) >= 0) foreignScore = 100;
        else if (foreign.compareTo(new BigDecimal("1")) >= 0) foreignScore = 50;
        else foreignScore = 10;

        // 4. Public Balance/Speculation Score (15%)
        double publicScore;
        if (publicHolding.compareTo(new BigDecimal("10")) < 0) publicScore = 30; // Liquidity Risk
        else if (publicHolding.compareTo(new BigDecimal("35")) <= 0) publicScore = 100; // Ideal Balance
        else if (publicHolding.compareTo(new BigDecimal("60")) <= 0) publicScore = 60; // Moderate Speculation
        else publicScore = 20; // High Speculative Risk

        int finalScore = (int) ((sponsorScore * 0.45) + (instScore * 0.30) + (foreignScore * 0.10) + (publicScore * 0.15));

        BigDecimal freeFloat = HUNDRED.subtract(sponsor);
        
        // Smart Reasoning Logic
        String summary;
        if (sponsor.compareTo(new BigDecimal("30")) < 0) {
            summary = "Weak Sponsor Confidence";
        } else if (sponsor.compareTo(new BigDecimal("85")) > 0) {
            summary = "Cornering / Liquidity Risk";
        } else if (finalScore >= 75) {
            summary = "Strong Institutional Base";
        } else if (publicHolding.compareTo(new BigDecimal("70")) > 0) {
            summary = "Speculative Structure";
        } else {
            summary = "Standard Ownership";
        }

        FundamentalAnalysisSection section = section("Ownership Quality", tone(finalScore), finalScore, summary);
        
        section.addCalculation("Free Float = 100% - Sponsor Holding = 100% - " + format(sponsor) + "% = " + format(freeFloat) + "%")
               .addCalculation("Strategic Holdings = Sponsor + Inst = " + format(sponsor) + "% + " + format(institute) + "% = " + format(sponsor.add(institute)) + "%")
               .addCalculation("Inst vs Public Ratio = Inst. Holding / Public Holding = " + format(institute) + "% / " + format(publicHolding) + "% = " + (publicHolding.compareTo(BigDecimal.ZERO) > 0 
                    ? format(institute.divide(publicHolding, 4, RoundingMode.HALF_UP)) : "N/A"));

        if (sponsor.compareTo(new BigDecimal("30")) < 0) {
            section.addReason("Sponsor holding is below the regulatory 30% guideline, indicating low skin-in-the-game.");
        } else if (sponsor.compareTo(new BigDecimal("85")) > 0) {
            section.addReason("Extremely high sponsor concentration may lead to low trading liquidity and high price volatility.");
        } else if (finalScore >= 75) {
            section.addReason("High sponsor and institutional participation supports long-term ownership stability.");
        } else if (publicHolding.compareTo(new BigDecimal("70")) > 0) {
            section.addReason("Ownership is heavily dominated by public retail, increasing the risk of speculative price swings.");
        } else {
            section.addReason("The ownership structure is balanced but lacks significant institutional or foreign conviction.");
        }

        // Risk Flags
        if (foreign.compareTo(new BigDecimal("0.1")) < 0) {
            section.addReason("Foreign ownership is negligible and does not materially impact institutional confidence.");
        }
        if (institute.compareTo(new BigDecimal("5")) < 0) {
            section.addReason("Warning: Negligible institutional presence suggests lack of professional fund support.");
        }

        return section;
    }

    private FundamentalAnalysisSection sizeSection(Company company) {
        BigDecimal marketCap = company.getMarketCapitalization();
        Long outstanding = company.getOutstandingSecurities();
        int score = marketCap == null ? 45
                : marketCap.compareTo(new BigDecimal("10000000000")) >= 0 ? 80
                : marketCap.compareTo(new BigDecimal("2000000000")) >= 0 ? 65
                : 45;

        return section("Size and Liquidity Proxy", tone(score), score,
                score >= 70 ? "Company size is relatively stronger." : "Company size may limit liquidity.")
                .addCalculation("Market capitalization = " + value(marketCap))
                .addCalculation("Outstanding securities = " + (outstanding != null ? outstanding : "-"))
                .addReason("Larger market capitalization often improves liquidity and reduces small-cap risk.")
                .addReason("This is a rough proxy; actual traded volume should also be checked separately.");
    }

    private FundamentalAnalysisSection section(String title, String tone, int score, String summary) {
        return new FundamentalAnalysisSection(title, tone, score, summary);
    }

    private List<EpsHistory> sortedEps(List<EpsHistory> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null && row.getYear() != null)
                .sorted(Comparator.comparingInt((EpsHistory row) -> parseYear(row.getYear())).reversed())
                .toList();
    }

    private List<DividendHistory> sortedDividends(List<DividendHistory> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null && row.getYear() != null)
                .sorted(Comparator.comparingInt((DividendHistory row) -> parseYear(row.getYear())).reversed())
                .toList();
    }

    private BigDecimal averageEps(List<EpsHistory> rows) {
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return rows.stream()
                .map(row -> nz(row.getEps()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(Math.max(1, rows.size())), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal latest, BigDecimal oldest) {
        if (oldest == null || oldest.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return latest.subtract(oldest)
                .divide(oldest.abs(), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    private int scoreByGrowth(BigDecimal growth, int positiveYears, int totalYears) {
        int score = growth.compareTo(new BigDecimal("25")) >= 0 ? 85
                : growth.compareTo(BigDecimal.ZERO) >= 0 ? 70
                : growth.compareTo(new BigDecimal("-20")) >= 0 ? 50
                : 30;
        if (positiveYears == totalYears) {
            score += 5;
        }
        return Math.min(score, 100);
    }

    private String decision(int score) {
        if (score >= 75) {
            return "Strong fundamentals";
        }
        if (score >= 60) {
            return "Acceptable fundamentals";
        }
        if (score >= 45) {
            return "Mixed fundamentals";
        }
        return "Weak fundamentals";
    }

    private String tone(int score) {
        if (score >= 75) {
            return "good";
        }
        if (score >= 60) {
            return "ok";
        }
        if (score >= 45) {
            return "warning";
        }
        return "bad";
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal parsePercent(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.replace(",", "").replace("%", "").trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int parseYear(String year) {
        try {
            return Integer.parseInt(year);
        } catch (Exception e) {
            return 0;
        }
    }

    private String value(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP).toPlainString() : "-";
    }

    private String format(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String percentText(BigDecimal value) {
        return value != null ? format(value) + "%" : "-";
    }
}
