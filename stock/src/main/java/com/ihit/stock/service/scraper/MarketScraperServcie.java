package com.ihit.stock.service.scraper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.ihit.stock.dto.StockDataForm;
import com.ihit.stock.service.StockMarketDataService;

@Service
public class MarketScraperServcie {

    private final StockMarketDataService stockMarketDataService;

    public MarketScraperServcie(StockMarketDataService stockMarketDataService) {
        this.stockMarketDataService = stockMarketDataService;
    }

    public void scrapeByDateRange(String tradingCode, LocalDate fromDate, LocalDate toDate, String userName) {
        try {
            String url = String.format(
                    "https://www.dsebd.org/day_end_archive.php?archive=view&startDate=%s&endDate=%s&inst=%s",
                    fromDate, toDate, tradingCode.trim().toUpperCase());

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();

            // The DSE historical table usually has class 'table-bordered'
            Element table = doc.selectFirst("div.searchArea");

            if (table == null) {
                throw new RuntimeException("Historical data table not found.");
            }
            // ====================================
            // Read Header Sequence Dynamically
            // ====================================
            Map<String, Integer> columnMap = new HashMap<>();

            Elements headers = table.select("thead th");
            for (int i = 0; i < headers.size(); i++) {

                String header = headers.get(i).text().trim().replace("*", "");
                columnMap.put(header, i);
            }

            // ====================================
            // Read Body Rows
            // ====================================
            Elements rows = table.select("tbody tr");

            List<StockDataForm> forms = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Element row : rows) {

                try {

                    Elements cols = row.select("td");

                    // Skip invalid rows
                    if (cols.size() < 12) {
                        continue;
                    }
                    StockDataForm form = new StockDataForm();

                    form.setDate(LocalDate.parse(cols.get(columnMap.get("DATE")).text().trim(), formatter));

                    form.setTradingCode(cols.get(columnMap.get("TRADING CODE")).text().trim());

                    form.setLtp(parseBigDecimal(cols.get(columnMap.get("LTP")).text()));

                    form.setHigh(parseBigDecimal(cols.get(columnMap.get("HIGH")).text()));

                    form.setLow(parseBigDecimal(cols.get(columnMap.get("LOW")).text()));

                    form.setOpenp(parseBigDecimal(cols.get(columnMap.get("OPENP")).text()));

                    form.setClosep(parseBigDecimal(cols.get(columnMap.get("CLOSEP")).text()));

                    form.setYcp(parseBigDecimal(cols.get(columnMap.get("YCP")).text()));
                    form.setTrade(parseBigDecimal(cols.get(columnMap.get("TRADE")).text()));
                    form.setTradeValue(parseBigDecimal(cols.get(columnMap.get("VALUE (mn)")).text()));
                    form.setVolume(parseLong(cols.get(columnMap.get("VOLUME")).text()));

                    forms.add(form);

                } catch (Exception rowEx) {
                    System.out.println("Row parsing failed: " + row.text());
                    System.out.println(rowEx.getMessage());
                }
                if (!forms.isEmpty()) {
                    stockMarketDataService.saveOrReplaceAll(forms);
                    System.out.println("Saved rows: " + forms.size());
                } else {
                    System.out.println("No historical data found for " + tradingCode);
                }
            }
            /*
             * List<StockDataForm> forms = new ArrayList<>();
             * DateTimeFormatter dseFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
             * 
             * for (Element row : rows) {
             * Elements cols = row.select("td");
             * if (cols.size() < 12) continue; // Skip header or incomplete rows
             * 
             * try {
             * StockDataForm form = new StockDataForm();
             * form.setDate(LocalDate.parse(cols.get(1).text().trim(), dseFormatter));
             * form.setTradingCode(cols.get(2).text().trim());
             * form.setLtp(parseBigDecimal(cols.get(3).text()));
             * form.setHigh(parseBigDecimal(cols.get(4).text()));
             * form.setLow(parseBigDecimal(cols.get(5).text()));
             * form.setOpenp(parseBigDecimal(cols.get(6).text()));
             * form.setClosep(parseBigDecimal(cols.get(7).text()));
             * form.setYcp(parseBigDecimal(cols.get(8).text()));
             * form.setTradeValue(parseBigDecimal(cols.get(10).text()));
             * form.setVolume(parseLong(cols.get(11).text()));
             * forms.add(form);
             * } catch (Exception e) {
             * // Skip rows that fail to parse (like headers with text in numeric columns)
             * }
             * }
             * 
             * if (!forms.isEmpty()) {
             * stockMarketDataService.saveOrReplaceAll(forms);
             * }
             */
        } catch (Exception e) {
            throw new RuntimeException("Historical scraping failed for " + tradingCode + ": " + e.getMessage(), e);
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        String clean = value.replace(",", "").trim();
        return (clean.isEmpty() || "-".equals(clean)) ? BigDecimal.ZERO : new BigDecimal(clean);
    }

    private Long parseLong(String value) {
        String clean = value.replace(",", "").trim();
        return (clean.isEmpty() || "-".equals(clean)) ? 0L : Long.parseLong(clean);
    }

}
