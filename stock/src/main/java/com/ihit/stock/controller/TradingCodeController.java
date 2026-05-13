package com.ihit.stock.controller;

import com.ihit.stock.service.TradingCodeService;
import com.ihit.stock.service.scraper.MarketScraperServcie;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/trading-code")
public class TradingCodeController {
    private final TradingCodeService tradingCodeService;
    private final MarketScraperServcie marketScraperServcie;


    public TradingCodeController(TradingCodeService tradingCodeService, MarketScraperServcie marketScraperServcie) {
        this.tradingCodeService = tradingCodeService;
        this.marketScraperServcie = marketScraperServcie;
    }

    @GetMapping
    public String view(@RequestParam(required = false) Long editId, Model model) {
        model.addAttribute("tradingCodes", tradingCodeService.findAll());
        if (editId != null) {
            model.addAttribute("editTradingCode", tradingCodeService.findById(editId));
        }
        return "trading-code";
    }

    @PostMapping("/save")
    public String save(@RequestParam String code, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            tradingCodeService.create(code, userName(principal));
            redirectAttributes.addFlashAttribute("success", "Trading code saved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trading-code";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @RequestParam String code, RedirectAttributes redirectAttributes) {
        try {
            tradingCodeService.update(id, code);
            redirectAttributes.addFlashAttribute("success", "Trading code updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trading-code";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tradingCodeService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Trading code deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trading-code";
    }

    @PostMapping("/{id}/scrape-save")
    public String scrapeAndSave(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String code = tradingCodeService.scrapeAndSave(id, userName(principal));
            redirectAttributes.addFlashAttribute("success", "Company data scraped and saved for " + code + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trading-code";
    }

    @PostMapping("/scrape-save-all")
    public String scrapeAndSaveAll(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String message = tradingCodeService.scrapeAndSaveAll(userName(principal));
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trading-code";
    }

    @PostMapping("/scrape-by-date-range")
    public String scrapeByDateRange(
            @RequestParam String tradingCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            marketScraperServcie.scrapeByDateRange(tradingCode, fromDate, toDate, userName(principal));
            redirectAttributes.addFlashAttribute("success", "Historical scraping initiated for " + tradingCode + " from " + fromDate + " to " + toDate + "." );
        
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stocks/market-data-scraping";
    }

    private String userName(Principal principal) {
        return principal != null ? principal.getName() : null;
    }
}
