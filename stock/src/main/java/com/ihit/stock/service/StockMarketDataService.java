package com.ihit.stock.service;

import com.ihit.stock.dto.StockDataForm;
import com.ihit.stock.model.StockMarketData;
import com.ihit.stock.repository.StockMarketDataRepository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMarketDataService {

    private final StockMarketDataRepository repository;

    public StockMarketDataService(StockMarketDataRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int saveOrReplaceAll(List<StockDataForm> forms) {
        for (StockDataForm form : forms) {
            StockMarketData data = repository.findByDateAndTradingCodeIgnoreCase(form.getDate(), form.getTradingCode())
                    .orElseGet(() -> new StockMarketData(form.getDate(), form.getTradingCode()));
            apply(data, form);
            repository.save(data);
        }
        return forms.size();
    }

    public Page<StockMarketData> findAll(String tradingCode, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From Date cannot be after To Date.");
        }

        return repository.findWithFilters(tradingCode, fromDate, toDate, pageable);
    }

    public StockDataForm findForm(Long id) {
        StockMarketData data = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found"));
        return toForm(data);
    }

    @Transactional
    public void update(Long id, StockDataForm form) {
        StockMarketData data = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found"));
        apply(data, form);
    }

    @Transactional
    public long deleteByTradingCode(String tradingCode) {
        if (tradingCode == null || tradingCode.isBlank()) {
            throw new IllegalArgumentException("Trading code is required");
        }
        return repository.deleteByTradingCodeIgnoreCase(tradingCode.trim());
    }

    private void apply(StockMarketData data, StockDataForm form) {
        if (form.getDate() == null) {
            throw new IllegalArgumentException("DATE is required");
        }
        if (form.getTradingCode() == null || form.getTradingCode().isBlank()) {
            throw new IllegalArgumentException("TRADING CODE is required");
        }
        data.update(
                form.getDate(),
                form.getTradingCode().trim(),
                form.getLtp(),
                form.getHigh(),
                form.getLow(),
                form.getOpenp(),
                form.getClosep(),
                form.getYcp(),
                form.getTrade(),
                form.getTradeValue(),
                form.getVolume());
    }

    private StockDataForm toForm(StockMarketData data) {
        StockDataForm form = new StockDataForm();
        form.setId(data.getId());
        form.setDate(data.getDate());
        form.setTradingCode(data.getTradingCode());
        form.setLtp(data.getLtp());
        form.setHigh(data.getHigh());
        form.setLow(data.getLow());
        form.setOpenp(data.getOpenp());
        form.setClosep(data.getClosep());
        form.setYcp(data.getYcp());
        form.setTrade(data.getTrade());
        form.setTradeValue(data.getTradeValue());
        form.setVolume(data.getVolume());
        return form;
    }
}
