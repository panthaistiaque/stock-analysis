package com.ihit.stock.service.scraper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.ihit.stock.dto.CompanyFundamentalDto;
import com.ihit.stock.dto.DividendDto;
import com.ihit.stock.dto.EpsDto;

@Service
public class CompanyScraperService {
    public CompanyFundamentalDto scrapeCompany(String tradingCode) {

        try {

            // 🔹 DSE company URL
            String url = "https://www.dsebd.org/displayCompany.php?name="
                    + tradingCode;

            // 🔹 Connect and fetch HTML
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .referrer("https://www.google.com")
                    .timeout(30000)
                    .followRedirects(true)
                    .ignoreContentType(true)
                    .get();

            Element section = doc.getElementById("section-to-print");
            if (section == null) {
                System.out.println(" section-to-print not found");
                return new CompanyFundamentalDto();
            }
            if (section.text().contains("No company found")) {
                throw new RuntimeException("Invalid trading code : " + tradingCode);
            }

            // 🔹 Print full page text (for testing)
            // System.out.println(section);
            CompanyFundamentalDto dto = new CompanyFundamentalDto();
            dto.setScrapingDate(LocalDateTime.now());

            // ===================================
            // COMPANY NAME
            // ===================================
            dto.setCompanyName(section.select("h2 i").first().text());

            // ===================================
            // TRADING CODE
            // ===================================
            dto.setTradingCode(tradingCode);

            // ===================================
            // SECTOR
            // ===================================
            dto.setSector(extractThValue(section, "Sector"));

            // ===================================
            // MARKET CATEGORY
            // ===================================
            dto.setMarketCategory(extractTdValue(section, "Market Category"));

            // ===================================
            // LAST TRADING PRICE
            // ===================================
            dto.setLastTradingPrice(extractThValue(section, "Last Trading Price"));

            // ===================================
            // MARKET CAPITALIZATION
            // ===================================
            dto.setMarketCapitalization(parseFinancialValue(extractThValue(section, "Market Capitalization (mn)")));

            // ===================================
            // PAID-UP CAPITAL
            // ===================================
            dto.setPaidUpCapital(parseFinancialValue(extractThValue(section, "Paid-up Capital (mn)")));
            // ===================================
            System.out.println(parseBigInteger(extractThValue(section, "Total No. of Outstanding Securities")));
            dto.setOutstandingSecurities(
                    parseBigInteger(extractThValue(section, "Total No. of Outstanding Securities")));
            // ===================================
            // 52 WEEK RANGE
            // ===================================
            dto.setWeek52Range(extractThValue(section, "52 Weeks' Moving Range"));

            // ===================================
            // PE RATIO
            // ===================================
            dto.setPeRatio(extractPeRatio(section));

            // ===================================
            // DIVIDEND YIELD
            // ===================================
            dto.setDividendYield(extractDividendYield(section));

            // ===================================
            // HOLDINGS
            // ===================================
            extractShareHolding(section, dto);
            extractDividendHistory(section, dto);
            extractLoanData(section, dto);
            extractEpsData(section, dto);
            return dto;

        } catch (Exception e) {

            throw new RuntimeException("Failed to scrape company : " + tradingCode, e);
        }
    }

    private String extractThValue(Element section, String label) {
        for (Element th : section.select("th")) {
            if (th.text().contains(label)) {
                Element next = th.nextElementSibling();
                if (next != null) {
                    if (label.contains("(mn)")) {
                        return next.text() + " mn";
                    }
                    return next.text();
                }
            }
        }
        return null;
    }

    private String extractTdValue(Element section, String label) {

        for (Element td : section.select("td")) {
            String text = td.text().trim();
            if (text.contains(label)) {
                Element next = td.nextElementSibling();
                if (next != null) {
                    return next.text().replace(",", "").trim();
                }
            }
        }

        return null;
    }

    private void extractShareHolding(Element section, CompanyFundamentalDto dto) {

        Element latestRow = null;

        // =====================================
        // FIND LAST SHARE HOLDING ROW
        // =====================================

        for (Element tr : section.select("tr")) {

            if (tr.text().contains("Share Holding Percentage")) {

                latestRow = tr;
            }
        }

        // no row found
        if (latestRow == null) {
            return;
        }

        // =====================================
        // PARSE LAST ROW
        // =====================================

        for (Element td : latestRow.select("table td")) {

            String text = td.text().replace("\n", " ").trim();

            String[] parts = text.split(":");

            if (parts.length < 2) {
                continue;
            }

            String label = parts[0].trim();

            String value = parts[1].trim();

            switch (label) {

                case "Sponsor/Director":
                    dto.setSponsorHolding(value);
                    break;

                case "Govt":
                    dto.setGovtHolding(value);
                    break;

                case "Institute":
                    dto.setInstituteHolding(value);
                    break;

                case "Foreign":
                    dto.setForeignHolding(value);
                    break;

                case "Public":
                    dto.setPublicHolding(value);
                    break;
            }
        }
    }

    private String extractPeRatio(
            Element section) {

        for (Element td : section.select("td")) {

            if (td.text()
                    .contains("Trailing P/E Ratio")) {

                Element next = td.nextElementSibling();

                if (next != null) {
                    return next.text();
                }
            }
        }

        return null;
    }

    private String extractDividendYield(
            Element section) {

        for (Element td : section.select("td")) {

            if (td.text()
                    .contains("Dividend Yield")) {

                Element next = td.nextElementSibling();

                if (next != null) {
                    return next.text();
                }
            }
        }

        return null;
    }

    // private String extractPercentage(String text) {

    // String[] parts = text.split(":");
    // if (parts.length > 1) {
    // return parts[1].replace("\n", "").trim();
    // }
    // return null;
    // }

    private BigDecimal parseFinancialValue(String value) {
        System.out.println(value);
        try {

            if (value == null || value.isBlank()) {

                return BigDecimal.ZERO;
            }

            // normalize
            value = value.trim().replace(",", "").toLowerCase();

            // =================================
            // DETECT UNIT
            // =================================

            BigDecimal multiplier = BigDecimal.ONE;

            if (value.contains("mn")) {
                multiplier = new BigDecimal("1000000");
                value = value.replace("mn", "").trim();
            }

            else if (value.contains("cr")) {
                multiplier = new BigDecimal("10000000");
                value = value.replace("cr", "").trim();
            }

            else if (value.contains("%")) {
                value = value.replace("%", "").trim();
            }

            BigDecimal number = new BigDecimal(value);
            System.out.println(number);
            System.out.println(number.multiply(multiplier));
            return number.multiply(multiplier);

        } catch (Exception e) {

            return BigDecimal.ZERO;
        }
    }

    private void extractDividendHistory(Element section, CompanyFundamentalDto dto) {

        List<DividendDto> dividends = new ArrayList<>();

        String cashDividendText = null;
        String stockDividendText = null;

        // =====================================
        // FIND DIVIDEND ROWS
        // =====================================
        for (Element tr : section.select("tr")) {

            // Elements tdList = tr.select("td");
            // Elements thList = tr.select("th");
            Element thList = tr.selectFirst("th");

            Element tdList = tr.selectFirst("td");

            if (thList == null || tdList == null) {
                continue;
            }

            String label = thList.text().trim();

            String value = tdList.text()
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            // =====================================
            // CASH DIVIDEND
            // =====================================

            if (label.equalsIgnoreCase("Cash Dividend")) {
                cashDividendText = value;
            }

            // =====================================
            // STOCK DIVIDEND
            // =====================================

            else if (label.equalsIgnoreCase("Bonus Issue (Stock Dividend)")) {
                stockDividendText = value;
            }
        }

        // =====================================
        // REGEX PATTERN
        // Example:
        // 25% 2025
        // 3.50% 2018
        // =====================================
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)%\\s*(\\d{4})");

        // =====================================
        // PROCESS CASH DIVIDEND
        // =====================================

        if (cashDividendText != null && !cashDividendText.equals("-")) {

            Matcher matcher = pattern.matcher(cashDividendText);

            while (matcher.find()) {
                String dividendValue = matcher.group(1);
                String year = matcher.group(2);
                DividendDto dividend = new DividendDto();
                dividend.setYear(year);
                dividend.setCashDividend(dividendValue);
                // default stock dividend
                dividend.setStockDividend("0");
                dividends.add(dividend);
            }
        }

        // =====================================
        // PROCESS STOCK DIVIDEND
        // =====================================

        if (stockDividendText != null && !stockDividendText.equals("-")) {

            Matcher matcher = pattern.matcher(stockDividendText);

            while (matcher.find()) {

                String stockValue = matcher.group(1);
                String year = matcher.group(2);
                // match existing year
                boolean found = false;
                for (DividendDto dividend : dividends) {

                    if (dividend.getYear().equals(year)) {
                        dividend.setStockDividend(stockValue);
                        found = true;
                        break;
                    }
                }

                // if no cash dividend exists
                // create new row

                if (!found) {
                    DividendDto dividend = new DividendDto();
                    dividend.setYear(year);
                    dividend.setCashDividend("0");
                    dividend.setStockDividend(stockValue);
                    dividends.add(dividend);
                }
            }
        }
        dividends.sort(Comparator.comparingInt((DividendDto dividend) -> parseYear(dividend.getYear())).reversed());
        dto.setDividends(dividends);
    }

    private void extractLoanData(Element section, CompanyFundamentalDto dto) {

        for (Element row : section.select("table#company tr")) {

            Elements tdList = row.select("td");

            if (tdList.isEmpty()) {
                continue;
            }

            String label = row.text();
            // =========================
            // SHORT TERM LOAN
            // =========================
            if (label.contains("Short-term loan")) {
                String value = tdList.last().text().replace(",", "").trim();
                dto.setShortTermLoan(parseFinancialValue(value + " mn"));
            }

            // =========================
            // LONG TERM LOAN
            // =========================
            else if (label.contains("Long-term loan")) {
                String value = tdList.last().text().replace(",", "").trim();
                dto.setLongTermLoan(parseFinancialValue(value + " mn"));
            }
        }
    }

    private BigInteger parseBigInteger(String value) {

        try {
            if (value == null || value.isBlank()) {
                return BigInteger.ZERO;
            }
            value = value.replace(",", "").trim();
            return new BigInteger(value);

        } catch (Exception e) {
            return BigInteger.ZERO;
        }
    }

    private void extractEpsData(
            Element section,
            CompanyFundamentalDto dto) {

        List<EpsDto> epsHistory = new ArrayList<>();

        // =====================================
        // FIND FINANCIAL PERFORMANCE TABLE
        // =====================================

        for (Element row : section.select("table tr")) {

            Elements tdList = row.select("td");

            // required minimum columns
            if (tdList.size() < 11) {
                continue;
            }

            // =====================================
            // YEAR
            // =====================================

            String year = tdList.get(0)
                    .text()
                    .trim();

            // skip non-year rows
            if (!year.matches("\\d{4}")) {
                continue;
            }

            // =====================================
            // EPS
            // EPS - Continuing Operations Basic Original
            // =====================================

            String epsText = tdList.get(4)
                    .text()
                    .replace(",", "")
                    .trim();

            // =====================================
            // NAV
            // =====================================

            String navText = tdList.get(7)
                    .text()
                    .replace(",", "")
                    .trim();

            // =====================================
            // PROFIT
            // =====================================

            String profitText = tdList.get(10)
                    .text()
                    .replace(",", "")
                    .trim();

            EpsDto item = new EpsDto();

            item.setYear(year);

            // EPS
            try {
                item.setEps(new BigDecimal(epsText));
            } catch (Exception e) {
                item.setEps(BigDecimal.ZERO);
            }

            // NAV
            try {
                item.setNavPerShare(new BigDecimal(navText));
            } catch (Exception e) {
                item.setNavPerShare(BigDecimal.ZERO);
            }

            // PROFIT
            try {
                item.setProfit(parseFinancialValue(profitText + "mn"));
            } catch (Exception e) {
                item.setProfit(BigDecimal.ZERO);
            }

            epsHistory.add(item);
        }

        epsHistory.sort(Comparator.comparingInt((EpsDto eps) -> parseYear(eps.getYear())).reversed());
        dto.setEpsHistory(epsHistory);
    }

    private int parseYear(String year) {
        try {
            return Integer.parseInt(year);
        } catch (Exception e) {
            return 0;
        }
    }
}
