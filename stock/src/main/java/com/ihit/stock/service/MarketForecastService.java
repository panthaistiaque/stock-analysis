package com.ihit.stock.service;

import com.ihit.stock.dto.MarketForecastRow;
import com.ihit.stock.model.MarketForecast;
import com.ihit.stock.model.StockMarketData;
import com.ihit.stock.repository.MarketForecastRepository;
import com.ihit.stock.repository.StockMarketDataRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketForecastService {

    private static final List<Integer> FORECAST_HORIZONS = Arrays.asList(7, 15, 30, 60);

    private final MarketForecastRepository forecastRepository;
    private final StockMarketDataRepository stockMarketDataRepository;

    public MarketForecastService(MarketForecastRepository forecastRepository,
            StockMarketDataRepository stockMarketDataRepository) {
        this.forecastRepository = forecastRepository;
        this.stockMarketDataRepository = stockMarketDataRepository;
    }

    public List<MarketForecastRow> generateForecastRows(String tradingCode, List<StockMarketData> priceHistory) {
        if (priceHistory == null || priceHistory.size() < 3) {
            return List.of();
        }

        List<StockMarketData> cleanHistory = priceHistory.stream()
                .filter(row -> row != null && row.getDate() != null && row.getLtp() != null)
                .toList();
        if (cleanHistory.size() < 3) {
            return List.of();
        }

        int n = cleanHistory.size();
        double[] prices = cleanHistory.stream()
                .mapToDouble(row -> row.getLtp().doubleValue())
                .toArray();
        WeightedRegression regression = calculateRegression(prices);
        if (regression == null) {
            return List.of();
        }

        LocalDate baseDate = cleanHistory.get(n - 1).getDate();
        List<MarketForecastRow> rows = new ArrayList<>();
        for (Integer horizon : FORECAST_HORIZONS) {
            int xVal = n + horizon - 1;
            double predicted = regression.slope() * xVal + regression.intercept();
            double timeExpansionFactor = Math.sqrt(1 + (1.0 / n)
                    + Math.pow(xVal - (n / 2.0), 2) / regression.sumWxx());
            double confidenceInterval = 1.96 * regression.residualStandardError() * timeExpansionFactor;

            rows.add(new MarketForecastRow(
                    tradingCode,
                    baseDate,
                    baseDate.plusDays(horizon),
                    horizon,
                    toMoney(predicted),
                    toMoney(predicted + confidenceInterval),
                    toMoney(Math.max(0, predicted - confidenceInterval)),
                    null,
                    null,
                    null,
                    null));
        }
        return rows;
    }

    @Transactional
    public int saveLatestForecast(String tradingCode, List<StockMarketData> priceHistory) {
        List<MarketForecastRow> forecastRows = generateForecastRows(tradingCode, priceHistory);
        for (MarketForecastRow row : forecastRows) {
            MarketForecast forecast = forecastRepository
                    .findByTradingCodeIgnoreCaseAndForecastBaseDateAndTargetDate(
                            row.getTradingCode(),
                            row.getForecastBaseDate(),
                            row.getTargetDate())
                    .orElseGet(MarketForecast::new);

            forecast.setTradingCode(row.getTradingCode());
            forecast.setForecastBaseDate(row.getForecastBaseDate());
            forecast.setTargetDate(row.getTargetDate());
            forecast.setHorizonDays(row.getHorizonDays());
            forecast.setPredictedPrice(row.getPredictedPrice());
            forecast.setUpperBound(row.getUpperBound());
            forecast.setLowerBound(row.getLowerBound());
            refreshActualPrice(forecast);
            forecastRepository.save(forecast);
        }
        return forecastRows.size();
    }

    @Transactional
    public List<MarketForecastRow> findSavedForecastRows(String tradingCode) {
        if (tradingCode == null || tradingCode.isBlank()) {
            return List.of();
        }
        List<MarketForecast> savedForecasts = forecastRepository
                .findAllByTradingCodeIgnoreCaseOrderByForecastBaseDateDescTargetDateAsc(tradingCode.trim());
        for (MarketForecast forecast : savedForecasts) {
            refreshActualPrice(forecast);
        }
        return savedForecasts.stream()
                .map(this::toRow)
                .toList();
    }

    private WeightedRegression calculateRegression(double[] prices) {
        int n = prices.length;
        double sumW = 0;
        double sumWX = 0;
        double sumWY = 0;
        double sumWXX = 0;
        double sumWXY = 0;

        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(prices[i])) {
                return null;
            }
            double weight = i > n * 0.8 ? 2.0 : 1.0;
            sumW += weight;
            sumWX += weight * i;
            sumWY += weight * prices[i];
            sumWXX += weight * i * i;
            sumWXY += weight * i * prices[i];
        }

        double denominator = sumW * sumWXX - sumWX * sumWX;
        if (denominator == 0) {
            return null;
        }

        double slope = (sumW * sumWXY - sumWX * sumWY) / denominator;
        double intercept = (sumWY - slope * sumWX) / sumW;
        double residualSumSquares = 0;
        for (int i = 0; i < n; i++) {
            double residual = prices[i] - (slope * i + intercept);
            residualSumSquares += residual * residual;
        }
        double residualStandardError = Math.sqrt(residualSumSquares / Math.max(1, n - 2));
        if (!Double.isFinite(slope) || !Double.isFinite(intercept) || !Double.isFinite(residualStandardError)) {
            return null;
        }
        return new WeightedRegression(slope, intercept, sumWXX, residualStandardError);
    }

    private void refreshActualPrice(MarketForecast forecast) {
        Optional<StockMarketData> actualData = stockMarketDataRepository
                .findFirstByTradingCodeIgnoreCaseAndDateGreaterThanEqualOrderByDateAsc(
                        forecast.getTradingCode(),
                        forecast.getTargetDate());
        if (actualData.isEmpty()) {
            return;
        }

        StockMarketData actual = actualData.get();
        if (actual.getLtp() == null) {
            return;
        }

        BigDecimal absoluteError = actual.getLtp().subtract(forecast.getPredictedPrice()).abs();
        BigDecimal percentageError = null;
        if (actual.getLtp().compareTo(BigDecimal.ZERO) > 0) {
            percentageError = absoluteError
                    .multiply(new BigDecimal("100"))
                    .divide(actual.getLtp(), 4, RoundingMode.HALF_UP);
        }

        forecast.setActualDate(actual.getDate());
        forecast.setActualPrice(actual.getLtp());
        forecast.setAbsoluteError(absoluteError);
        forecast.setPercentageError(percentageError);
    }

    private MarketForecastRow toRow(MarketForecast forecast) {
        return new MarketForecastRow(
                forecast.getTradingCode(),
                forecast.getForecastBaseDate(),
                forecast.getTargetDate(),
                forecast.getHorizonDays(),
                forecast.getPredictedPrice(),
                forecast.getUpperBound(),
                forecast.getLowerBound(),
                forecast.getActualDate(),
                forecast.getActualPrice(),
                forecast.getAbsoluteError(),
                forecast.getPercentageError());
    }

    private BigDecimal toMoney(double value) {
        if (!Double.isFinite(value)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record WeightedRegression(double slope, double intercept, double sumWxx, double residualStandardError) {
    }
}
