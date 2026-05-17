package com.ihit.stock.service;

import com.ihit.stock.dto.MissingMarketDataDto;
import com.ihit.stock.dto.SectorCoverageDto;
import com.ihit.stock.dto.SmartAlertDto;
import com.ihit.stock.model.Company;
import com.ihit.stock.model.StockMarketData;
import com.ihit.stock.repository.CompanyRepository;
import com.ihit.stock.repository.MarketForecastRepository;
import com.ihit.stock.repository.StockMarketDataRepository;
import com.ihit.stock.repository.TradingCodeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final StockMarketDataRepository stockMarketDataRepository;
    private final MarketForecastRepository marketForecastRepository;
    private final TradingCodeRepository tradingCodeRepository;
    private final CompanyRepository companyRepository;

    public DashboardService(StockMarketDataRepository stockMarketDataRepository,
            MarketForecastRepository marketForecastRepository,
            TradingCodeRepository tradingCodeRepository,
            CompanyRepository companyRepository) {
        this.stockMarketDataRepository = stockMarketDataRepository;
        this.marketForecastRepository = marketForecastRepository;
        this.tradingCodeRepository = tradingCodeRepository;
        this.companyRepository = companyRepository;
    }

    public List<TradingCodeRepository.TradingCodeProjection> findFundamentalTradingCodeDetails() {
        return tradingCodeRepository.findFundamentalTradingCodeDetails();
    }

    public long countFundamentalCompanyRows() {
        return companyRepository.count();
    }

    public List<MissingMarketDataDto> findMissingMarketData(List<String> allTradingCodes) {
        List<MissingMarketDataDto> missingDataList = new ArrayList<>();
        
        // Benchmark against the latest date in DB OR the last expected business day
        LocalDate latestInDb = stockMarketDataRepository.findOverallMaxDate().orElse(null);
        LocalDate lastBusinessDay = getPreviousBusinessDay(LocalDate.now());
        
        // The target is the most recent completed market session (system-wide or real-world)
        LocalDate benchmarkDate = (latestInDb == null || lastBusinessDay.isAfter(latestInDb)) 
                ? lastBusinessDay : latestInDb;

        for (String tradingCode : allTradingCodes) {
            Optional<LocalDate> lastRecordedDateOptional = stockMarketDataRepository.findMaxDateByTradingCode(tradingCode);

            if (lastRecordedDateOptional.isEmpty() || lastRecordedDateOptional.get().isBefore(benchmarkDate)) {
                LocalDate lastDate = lastRecordedDateOptional.orElse(null);
                LocalDate missingFrom = (lastDate != null) ? getNextBusinessDay(lastDate) : null;
                missingDataList.add(new MissingMarketDataDto(tradingCode, lastDate, missingFrom));
            }
        }

        return missingDataList;
    }

    public List<LocalDate> getMissingMarketDates() {
        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate latest = getLatestMarketDate();
        if (latest == null) return missingDates;

        LocalDate current = getNextBusinessDay(latest);
        LocalDate today = LocalDate.now();

        while (current.isBefore(today)) {
            missingDates.add(current);
            current = getNextBusinessDay(current);
        }
        return missingDates;
    }

    public String getMarketStatusMessage() {
        LocalDate latest = getLatestMarketDate();
        if (latest == null) return "No Data Found";

        LocalDate today = LocalDate.now();
        long daysAgo = ChronoUnit.DAYS.between(latest, today);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        String dateStr = latest.format(formatter);

        if (daysAgo == 0) return "Last Session: Today (" + dateStr + ")";

        String timeSuffix = daysAgo + " day" + (daysAgo > 1 ? "s" : "") + " ago";
        
        // Logic for weekends
        boolean isWeekend = today.getDayOfWeek() == DayOfWeek.FRIDAY || today.getDayOfWeek() == DayOfWeek.SATURDAY;
        if (isWeekend) {
            return String.format("Last Session: %s (%s - Weekend)", dateStr, timeSuffix);
        }

        return String.format("Last Session: %s (%s)", dateStr, timeSuffix);
    }

    private LocalDate getPreviousBusinessDay(LocalDate date) {
        LocalDate prev = date.minusDays(1);
        while (prev.getDayOfWeek() == DayOfWeek.FRIDAY || prev.getDayOfWeek() == DayOfWeek.SATURDAY) {
            prev = prev.minusDays(1);
        }
        return prev;
    }

    private LocalDate getNextBusinessDay(LocalDate date) {
        LocalDate next = date.plusDays(1);
        while (next.getDayOfWeek() == DayOfWeek.FRIDAY || next.getDayOfWeek() == DayOfWeek.SATURDAY) {
            next = next.plusDays(1);
        }
        return next;
    }

    public LocalDate getLatestMarketDate() {
        return stockMarketDataRepository.findOverallMaxDate().orElse(null);
    }

    public boolean isSyncRequired() {
        LocalDate latest = getLatestMarketDate();
        if (latest == null) return true;

        LocalDate today = LocalDate.now();
        // Assuming Friday (5) and Saturday (6) are weekends where no data is expected
        if (today.getDayOfWeek() == DayOfWeek.FRIDAY || today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return false;
        }
        return latest.isBefore(today);
    }

    public long countMarketRecords() {
        return stockMarketDataRepository.count();
    }

    public int countTradingCodesWithMarketData() {
        return stockMarketDataRepository.findDistinctTradingCodes().size();
    }

    public List<StockMarketDataRepository.MarketCoverageProjection> findMarketCoverageSummary() {
        return stockMarketDataRepository.findMarketCoverageSummary();
    }

    public long countSavedForecasts() {
        return marketForecastRepository.count();
    }

    public long countVerifiedForecasts() {
        return marketForecastRepository.countByActualPriceIsNotNull();
    }

    public List<MarketForecastRepository.ForecastCoverageProjection> findForecastCoverageSummary() {
        return marketForecastRepository.findForecastCoverageSummary();
    }

    public List<SectorCoverageDto> findSectorCoverage() {
        Map<String, Long> sectorCounts = companyRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        company -> cleanSector(company.getSector()),
                        Collectors.counting()));

        return sectorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new SectorCoverageDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<SmartAlertDto> findSmartAlerts() {
        Map<String, List<StockMarketData>> historyByCode = stockMarketDataRepository.findAllByOrderByDateDescTradingCodeAsc()
                .stream()
                .collect(Collectors.groupingBy(
                        row -> row.getTradingCode() == null ? "" : row.getTradingCode().trim().toUpperCase(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<SmartAlertDto> alerts = new ArrayList<>();
        for (Company company : companyRepository.findAll()) {
            String code = company.getTradingCode() == null ? "" : company.getTradingCode().trim().toUpperCase();
            List<StockMarketData> descendingHistory = historyByCode.getOrDefault(code, List.of());
            List<StockMarketData> ascendingHistory = descendingHistory.stream()
                    .sorted(Comparator.comparing(StockMarketData::getDate))
                    .toList();
            StockMarketData latestMarket = ascendingHistory.isEmpty() ? null : ascendingHistory.get(ascendingHistory.size() - 1);

            addPeAlerts(alerts, company, latestMarket);
            addMarketAlerts(alerts, code, ascendingHistory);
        }

        alerts.sort(Comparator
                .comparing(SmartAlertDto::getTriggerDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(this::severityRank)
                .thenComparing(SmartAlertDto::getTradingCode, Comparator.nullsLast(String::compareToIgnoreCase)));
        return alerts;
    }

    public double calculateCoveragePercent(int completed, int total) {
        if (total <= 0) {
            return 0;
        }
        return (completed * 100.0) / total;
    }

    private String cleanSector(String sector) {
        if (sector == null || sector.isBlank()) {
            return "Unclassified";
        }
        return sector.trim();
    }

    private void addPeAlerts(List<SmartAlertDto> alerts, Company company, StockMarketData latestMarket) {
        if (company.getPeRatio() == null || company.getTradingCode() == null) {
            return;
        }
        LocalDate triggerDate = latestMarket != null ? latestMarket.getDate() : LocalDate.now();
        BigDecimal currentPrice = latestMarket != null ? latestMarket.getLtp() : company.getLastTradingPrice();

        if (company.getPeRatio().compareTo(new BigDecimal("40")) > 0) {
            alerts.add(new SmartAlertDto(
                    "PE unusually high",
                    company.getTradingCode(),
                    "PE ratio significantly above normal threshold",
                    "BEARISH",
                    triggerDate,
                    currentPrice,
                    82));
        } else if (company.getPeRatio().compareTo(BigDecimal.ZERO) > 0
                && company.getPeRatio().compareTo(new BigDecimal("10")) < 0) {
            alerts.add(new SmartAlertDto(
                    "PE unusually low",
                    company.getTradingCode(),
                    "PE ratio is below normal threshold",
                    "BULLISH",
                    triggerDate,
                    currentPrice,
                    72));
        }
    }

    private void addMarketAlerts(List<SmartAlertDto> alerts, String code, List<StockMarketData> history) {
        if (code.isBlank() || history.size() < 2) {
            return;
        }

        StockMarketData latest = history.get(history.size() - 1);
        StockMarketData previous = history.get(history.size() - 2);
        BigDecimal latestPrice = latest.getLtp();
        if (latestPrice == null) {
            return;
        }

        addGapAlerts(alerts, code, latest, latestPrice);
        addVolumeSpikeAlert(alerts, code, history, latest);
        addBreakoutAlerts(alerts, code, history, latest, latestPrice);
        addRsiAlerts(alerts, code, history, latest, latestPrice);

        if (previous.getLtp() != null && previous.getLtp().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal changePercent = latestPrice.subtract(previous.getLtp())
                    .multiply(new BigDecimal("100"))
                    .divide(previous.getLtp(), 2, RoundingMode.HALF_UP);
            if (changePercent.compareTo(new BigDecimal("5")) >= 0) {
                alerts.add(new SmartAlertDto("Price breakout", code,
                        "Strong daily price move of " + changePercent.toPlainString() + "%",
                        "BULLISH", latest.getDate(), latestPrice, 70));
            } else if (changePercent.compareTo(new BigDecimal("-5")) <= 0) {
                alerts.add(new SmartAlertDto("Support breakdown", code,
                        "Sharp daily price drop of " + changePercent.toPlainString() + "%",
                        "BEARISH", latest.getDate(), latestPrice, 70));
            }
        }
    }

    private void addGapAlerts(List<SmartAlertDto> alerts, String code, StockMarketData latest, BigDecimal latestPrice) {
        if (latest.getOpenp() == null || latest.getYcp() == null || latest.getYcp().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal gapPercent = latest.getOpenp().subtract(latest.getYcp())
                .multiply(new BigDecimal("100"))
                .divide(latest.getYcp(), 2, RoundingMode.HALF_UP);
        if (gapPercent.compareTo(new BigDecimal("3")) >= 0) {
            alerts.add(new SmartAlertDto("Gap up", code,
                    "Opening price gapped up " + gapPercent.toPlainString() + "%",
                    "BULLISH", latest.getDate(), latestPrice, 68));
        } else if (gapPercent.compareTo(new BigDecimal("-3")) <= 0) {
            alerts.add(new SmartAlertDto("Gap down", code,
                    "Opening price gapped down " + gapPercent.toPlainString() + "%",
                    "BEARISH", latest.getDate(), latestPrice, 68));
        }
    }

    private void addVolumeSpikeAlert(List<SmartAlertDto> alerts, String code, List<StockMarketData> history, StockMarketData latest) {
        if (latest.getVolume() == null || history.size() < 6) {
            return;
        }
        List<StockMarketData> previousRows = history.subList(Math.max(0, history.size() - 21), history.size() - 1);
        double averageVolume = previousRows.stream()
                .filter(row -> row.getVolume() != null)
                .mapToLong(StockMarketData::getVolume)
                .average()
                .orElse(0);
        if (averageVolume <= 0) {
            return;
        }
        double spikePercent = ((latest.getVolume() - averageVolume) / averageVolume) * 100;
        if (spikePercent >= 100) {
            alerts.add(new SmartAlertDto("Sudden volume spike", code,
                    "Volume spike +" + Math.round(spikePercent) + "%",
                    "WARNING", latest.getDate(), latest.getLtp(), 76));
        }
    }

    private void addBreakoutAlerts(List<SmartAlertDto> alerts, String code, List<StockMarketData> history,
            StockMarketData latest, BigDecimal latestPrice) {
        if (history.size() < 10) {
            return;
        }
        List<StockMarketData> priorRows = history.subList(Math.max(0, history.size() - 53), history.size() - 1);
        Optional<BigDecimal> priorHigh = priorRows.stream()
                .map(StockMarketData::getHigh)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder());
        Optional<BigDecimal> priorLow = priorRows.stream()
                .map(StockMarketData::getLow)
                .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.naturalOrder());

        if (priorHigh.isPresent() && latestPrice.compareTo(priorHigh.get()) > 0) {
            alerts.add(new SmartAlertDto("New 52-week high", code,
                    "Price moved above recent high " + priorHigh.get().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    "BULLISH", latest.getDate(), latestPrice, 84));
        }
        if (priorLow.isPresent() && latestPrice.compareTo(priorLow.get()) < 0) {
            alerts.add(new SmartAlertDto("New 52-week low", code,
                    "Price moved below recent low " + priorLow.get().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    "BEARISH", latest.getDate(), latestPrice, 84));
        }
    }

    private void addRsiAlerts(List<SmartAlertDto> alerts, String code, List<StockMarketData> history,
            StockMarketData latest, BigDecimal latestPrice) {
        if (history.size() < 15) {
            return;
        }
        BigDecimal rsi = calculateRsi(history.subList(history.size() - 15, history.size()));
        if (rsi == null) {
            return;
        }
        if (rsi.compareTo(new BigDecimal("70")) >= 0) {
            alerts.add(new SmartAlertDto("RSI overbought", code,
                    "RSI is overbought at " + rsi.toPlainString(),
                    "WARNING", latest.getDate(), latestPrice, 74));
        } else if (rsi.compareTo(new BigDecimal("30")) <= 0) {
            alerts.add(new SmartAlertDto("RSI oversold", code,
                    "RSI is oversold at " + rsi.toPlainString(),
                    "BULLISH", latest.getDate(), latestPrice, 74));
        }
    }

    private BigDecimal calculateRsi(List<StockMarketData> rows) {
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).getLtp() == null || rows.get(i - 1).getLtp() == null) {
                return null;
            }
            BigDecimal diff = rows.get(i).getLtp().subtract(rows.get(i - 1).getLtp());
            if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                gains = gains.add(diff);
            } else {
                losses = losses.add(diff.abs());
            }
        }
        if (losses.compareTo(BigDecimal.ZERO) == 0 && gains.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("50.00");
        }
        if (losses.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100.00");
        }
        BigDecimal rs = gains.divide(losses, 4, RoundingMode.HALF_UP);
        return new BigDecimal("100")
                .subtract(new BigDecimal("100").divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP));
    }

    private int severityRank(SmartAlertDto alert) {
        if ("BEARISH".equalsIgnoreCase(alert.getSeverity())) {
            return 0;
        }
        if ("WARNING".equalsIgnoreCase(alert.getSeverity())) {
            return 1;
        }
        return 2;
    }
}
