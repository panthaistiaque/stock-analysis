package com.ihit.stock.service;

import com.ihit.stock.dto.StockDataForm;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelStockDataParser {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("M-d-yyyy")
    };

    public List<StockDataForm> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an Excel file");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = readHeaders(sheet.getRow(0));
            List<StockDataForm> records = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                records.add(readRecord(row, headers, rowIndex + 1));
            }
            return records;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read Excel file");
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Excel file");
        }
    }

    private Map<String, Integer> readHeaders(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Header row is missing");
        }

        Map<String, Integer> headers = new HashMap<>();
        for (Cell cell : row) {
            headers.put(normalizeHeader(asText(cell)), cell.getColumnIndex());
        }

        requireHeader(headers, "DATE");
        requireHeader(headers, "TRADING CODE");
        return headers;
    }

    private StockDataForm readRecord(Row row, Map<String, Integer> headers, int displayRowNumber) {
        StockDataForm form = new StockDataForm();
        form.setDate(readDate(cell(row, headers, "DATE"), displayRowNumber));
        form.setTradingCode(requireText(cell(row, headers, "TRADING CODE"), "TRADING CODE", displayRowNumber));
        form.setLtp(readBigDecimal(cell(row, headers, "LTP")));
        form.setHigh(readBigDecimal(cell(row, headers, "HIGH")));
        form.setLow(readBigDecimal(cell(row, headers, "LOW")));
        form.setOpenp(readBigDecimal(cell(row, headers, "OPENP")));
        form.setClosep(readBigDecimal(cell(row, headers, "CLOSEP")));
        form.setYcp(readBigDecimal(cell(row, headers, "YCP")));
        form.setTradeValue(readBigDecimal(cell(row, headers, "TRADE VALUE")));
        form.setVolume(readLong(cell(row, headers, "VOLUME")));
        return form;
    }

    private Cell cell(Row row, Map<String, Integer> headers, String name) {
        Integer index = headers.get(normalizeHeader(name));
        if (index == null) {
            return null;
        }
        return row.getCell(index);
    }

    private LocalDate readDate(Cell cell, int displayRowNumber) {
        if (cell == null) {
            throw new IllegalArgumentException("DATE is required at row " + displayRowNumber);
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String text = asText(cell);
        if (text.isBlank()) {
            throw new IllegalArgumentException("DATE is required at row " + displayRowNumber);
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Invalid DATE at row " + displayRowNumber);
    }

    private String requireText(Cell cell, String fieldName, int displayRowNumber) {
        String text = asText(cell).trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required at row " + displayRowNumber);
        }
        return text;
    }

    private BigDecimal readBigDecimal(Cell cell) {
        String text = asText(cell).replace(",", "").trim();
        if (text.isBlank()) {
            return null;
        }
        return new BigDecimal(text);
    }

    private Long readLong(Cell cell) {
        String text = asText(cell).replace(",", "").trim();
        if (text.isBlank()) {
            return null;
        }
        return new BigDecimal(text).longValue();
    }

    private String asText(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            BigDecimal value = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            return value.toPlainString();
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return Boolean.toString(cell.getBooleanCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        return "";
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (!asText(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private void requireHeader(Map<String, Integer> headers, String name) {
        if (!headers.containsKey(normalizeHeader(name))) {
            throw new IllegalArgumentException("Missing required column: " + name);
        }
    }

    private String normalizeHeader(String header) {
        return header.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
