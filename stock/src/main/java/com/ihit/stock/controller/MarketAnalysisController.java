package com.ihit.stock.controller;

import com.ihit.stock.model.Company;
import com.ihit.stock.model.EpsHistory;
import com.ihit.stock.model.DividendHistory;
import com.ihit.stock.model.StockMarketData;
import com.ihit.stock.repository.StockMarketDataRepository;
import com.ihit.stock.service.CompanyService;
import com.ihit.stock.service.MarketForecastService;
import com.ihit.stock.service.TradingCodeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/market-analysis")
public class MarketAnalysisController {

    private final TradingCodeService tradingCodeService;
    private final CompanyService companyService;
    private final StockMarketDataRepository stockMarketDataRepository;
    private final MarketForecastService marketForecastService;

    public MarketAnalysisController(TradingCodeService tradingCodeService, 
                                   CompanyService companyService, 
                                   StockMarketDataRepository stockMarketDataRepository,
                                   MarketForecastService marketForecastService) {
        this.tradingCodeService = tradingCodeService;
        this.companyService = companyService;
        this.stockMarketDataRepository = stockMarketDataRepository;
        this.marketForecastService = marketForecastService;
    }

    @GetMapping
    public String analysis(@RequestParam(required = false) String tradingCode, Model model) {
        model.addAttribute("tradingCodes", tradingCodeService.getAllTradingCodes());
        model.addAttribute("selectedCode", tradingCode);

        if (tradingCode != null && !tradingCode.isBlank()) {
            try {
                String cleanCode = tradingCode.trim().toUpperCase();
                Company company = companyService.findByTradingCode(cleanCode);
                List<StockMarketData> priceHistory = stockMarketDataRepository.findAllByTradingCodeIgnoreCaseOrderByDateAsc(cleanCode);
                
                List<EpsHistory> sortedEps = safeList(company.getEpsHistory()).stream()
                        .sorted(Comparator.comparingInt(row -> parseYear(row.getYear())))
                        .collect(Collectors.toList());
                
                List<DividendHistory> sortedDividends = safeList(company.getDividends()).stream()
                        .sorted(Comparator.comparingInt(row -> parseYear(row.getYear())))
                        .collect(Collectors.toList());

                // Auto-calculate missing KPIs
                boolean peRatioManuallyCalculated = calculateMissingMetrics(company, sortedEps, sortedDividends);

                model.addAttribute("company", company);
                model.addAttribute("priceHistory", priceHistory);
                model.addAttribute("epsHistory", sortedEps);
                model.addAttribute("dividendHistory", sortedDividends);
                model.addAttribute("peRatioManuallyCalculated", peRatioManuallyCalculated);
                loadForecastAttributes(cleanCode, priceHistory, model);
            } catch (Exception e) {
                model.addAttribute("error", "Analysis data not found for: " + tradingCode + ". Ensure you have scraped company and market data.");
            }
        }
        return "market-analysis";
    }

    @PostMapping("/forecast/save")
    public String saveForecast(@RequestParam String tradingCode, RedirectAttributes redirectAttributes) {
        if (tradingCode == null || tradingCode.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Trading code is required before saving forecast.");
            return "redirect:/market-analysis";
        }

        String cleanCode = tradingCode.trim().toUpperCase();
        try {
            List<StockMarketData> priceHistory = stockMarketDataRepository.findAllByTradingCodeIgnoreCaseOrderByDateAsc(cleanCode);
            int saved = marketForecastService.saveLatestForecast(cleanCode, priceHistory);
            if (saved == 0) {
                redirectAttributes.addFlashAttribute("error", "At least 3 market price rows are required to save a forecast for " + cleanCode + ".");
            } else {
                redirectAttributes.addFlashAttribute("success", saved + " forecast rows saved for " + cleanCode + ".");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not save forecast for " + cleanCode + ": " + e.getMessage());
        }
        return "redirect:/market-analysis?tradingCode=" + cleanCode;
    }

    private void loadForecastAttributes(String tradingCode, List<StockMarketData> priceHistory, Model model) {
        model.addAttribute("forecastRows", Collections.emptyList());
        model.addAttribute("savedForecastRows", Collections.emptyList());

        try {
            model.addAttribute("forecastRows", marketForecastService.generateForecastRows(tradingCode, priceHistory));
            model.addAttribute("savedForecastRows", marketForecastService.findSavedForecastRows(tradingCode));
        } catch (Exception e) {
            model.addAttribute("forecastWarning", "Forecast verification is not ready yet. Save a forecast after the forecast table is available.");
        }
    }

    private boolean calculateMissingMetrics(Company company, List<EpsHistory> sortedEps, List<DividendHistory> sortedDividends) {
        boolean peRatioManuallyCalculated = false;
        BigDecimal ltp = company.getLastTradingPrice();
        if (ltp == null || ltp.compareTo(BigDecimal.ZERO) <= 0) return peRatioManuallyCalculated;
    
        // Calculate PE Ratio if missing
        if (company.getPeRatio() == null || company.getPeRatio().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal latestEps = sortedEps.isEmpty() ? null : sortedEps.get(sortedEps.size() - 1).getEps();
            if (latestEps != null && latestEps.compareTo(BigDecimal.ZERO) > 0) {
                company.setPeRatio(ltp.divide(latestEps, 2, RoundingMode.HALF_UP));
                peRatioManuallyCalculated = true;
            }
        }
    
        // Calculate Dividend Yield if missing
        if (company.getDividendYield() == null || company.getDividendYield().compareTo(BigDecimal.ZERO) <= 0) {
            DividendHistory latestDiv = sortedDividends.isEmpty() ? null : sortedDividends.get(sortedDividends.size() - 1);
            String cashDivStr = (latestDiv != null) ? latestDiv.getCashDividend() : null;
            if (cashDivStr != null && !cashDivStr.isBlank() && !"-".equals(cashDivStr)) {
                try {
                    BigDecimal cashPercent = new BigDecimal(cashDivStr.replace("%", "").replace(",", "").trim());
                    // Yield = (Cash% * FaceValue 10) / LTP
                    BigDecimal yield = cashPercent.multiply(new BigDecimal("10")).divide(ltp, 2, RoundingMode.HALF_UP);
                    company.setDividendYield(yield);
                } catch (NumberFormatException ignored) {
                    // Handle cases where cashDivStr might not be a valid number
                }
            }
        }
        return peRatioManuallyCalculated;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? Collections.emptyList() : rows;
    }

    private int parseYear(String year) {
        if (year == null) {
            return 0;
        }
        try {
            String digits = year.replaceAll("\\D+", "");
            if (digits.length() >= 4) {
                return Integer.parseInt(digits.substring(0, 4));
            }
            return digits.isBlank() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }
}
