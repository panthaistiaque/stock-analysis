package com.ihit.stock.controller;

import com.ihit.stock.service.TradingCodeService;
import com.ihit.stock.service.scraper.MarketScraperServcie;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String view(
            @RequestParam(required = false) Long editId,
            @RequestParam(required = false) String tradingCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("code").ascending());
        Page<com.ihit.stock.model.TradingCode> tradingCodePage = tradingCodeService.findAll(tradingCode, pageable);

        model.addAttribute("tradingCodePage", tradingCodePage);
        model.addAttribute("tradingCodes", tradingCodePage.getContent());
        
        if (editId != null) {
            model.addAttribute("editTradingCode", tradingCodeService.findById(editId));
        }
        model.addAttribute("tradingCode", tradingCode);
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
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            marketScraperServcie.scrapeByDateRange(tradingCode, fromDate, toDate, userName(principal));
            redirectAttributes.addFlashAttribute("success", "Historical dataload completed for " + tradingCode + " from " + fromDate + " to " + toDate + ".");
            
            // Pass parameters to the redirect URL to auto-filter the results
            redirectAttributes.addAttribute("tradingCode", tradingCode);
            redirectAttributes.addAttribute("fromDate", fromDate);
            redirectAttributes.addAttribute("toDate", toDate);
        
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stocks/data";
    }

    private String userName(Principal principal) {
        return principal != null ? principal.getName() : null;
    }
}
