package com.ihit.stock.controller;

import com.ihit.stock.dto.StockDataForm;
import com.ihit.stock.model.StockMarketData;
import com.ihit.stock.service.ExcelStockDataParser;
import com.ihit.stock.service.StockMarketDataService;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stocks")
public class StockDataController {

    private static final String PREVIEW_SESSION_KEY = "stockPreviewRows";

    private final ExcelStockDataParser parser;
    private final StockMarketDataService stockMarketDataService;

    public StockDataController(ExcelStockDataParser parser, StockMarketDataService stockMarketDataService) {
        this.parser = parser;
        this.stockMarketDataService = stockMarketDataService;
    }

    @GetMapping("/upload")
    public String uploadPage(Principal principal, Authentication authentication, Model model, HttpSession session) {
        // addShellAttributes(principal, authentication, model);
        model.addAttribute("previewRows", previewRows(session));
        return "stock-upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            List<StockDataForm> rows = parser.parse(file);
            session.setAttribute(PREVIEW_SESSION_KEY, rows);
            redirectAttributes.addFlashAttribute("success", rows.size() + " records parsed. Review before saving.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/stocks/upload";
    }

    @PostMapping("/save")
    public String savePreview(HttpSession session, RedirectAttributes redirectAttributes) {
        List<StockDataForm> rows = previewRows(session);
        if (rows.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No preview data found. Upload an Excel file first.");
            return "redirect:/stocks/upload";
        }

        int saved = stockMarketDataService.saveOrReplaceAll(rows);
        session.removeAttribute(PREVIEW_SESSION_KEY);
        redirectAttributes.addFlashAttribute("success",
                saved + " records saved. Existing date/code records were replaced.");
        return "redirect:/stocks/data";
    }

    @GetMapping("/data")
    public String savedData(
            @RequestParam(required = false) String tradingCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // Default current date if null
        if (fromDate == null) {
            fromDate = LocalDate.now();
        }

        if (toDate == null) {
            toDate = LocalDate.now();
        }

        try {
            Page<StockMarketData> stockPage = stockMarketDataService.findAll(tradingCode, fromDate, toDate, pageable);
            model.addAttribute("stockPage", stockPage);
            model.addAttribute("stocks", stockPage.getContent());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("stockPage", Page.empty(pageable));
            model.addAttribute("stocks", Collections.emptyList());
        }

        model.addAttribute("tradingCode", tradingCode);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "stock-data";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Principal principal, Authentication authentication, Model model) {
        // addShellAttributes(principal, authentication, model);
        model.addAttribute("stockDataForm", stockMarketDataService.findForm(id));
        return "stock-edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute StockDataForm form,
            RedirectAttributes redirectAttributes) {
        try {
            stockMarketDataService.update(id, form);
            redirectAttributes.addFlashAttribute("success", "Stock record updated.");
            return "redirect:/stocks/data";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/stocks/" + id + "/edit";
        }
    }

    @PostMapping("/delete-by-code")
    public String deleteByTradingCode(@RequestParam String tradingCode, RedirectAttributes redirectAttributes) {
        try {
            long deleted = stockMarketDataService.deleteByTradingCode(tradingCode);
            redirectAttributes.addFlashAttribute("success",
                    deleted + " records deleted for trading code " + tradingCode.trim() + ".");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/stocks/data";
    }

    @SuppressWarnings("unchecked")
    private List<StockDataForm> previewRows(HttpSession session) {
        Object rows = session.getAttribute(PREVIEW_SESSION_KEY);
        if (rows instanceof List<?>) {
            return (List<StockDataForm>) rows;
        }
        return Collections.emptyList();
    }

    @GetMapping("/market-data-scraping")
    public String marketDataScrapingPage() {
        return "market-data-scraping";
    }
}
