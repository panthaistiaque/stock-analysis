package com.ihit.stock.controller;

import com.ihit.stock.dto.CompanyFundamentalDto;
import com.ihit.stock.model.Company;
import com.ihit.stock.model.DividendHistory;
import com.ihit.stock.model.EpsHistory;
import com.ihit.stock.service.CompanyService;
import com.ihit.stock.service.scraper.CompanyScraperService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyScraperService scraperService;

    public CompanyController(CompanyService companyService, CompanyScraperService scraperService) {
        this.companyService = companyService;
        this.scraperService = scraperService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parseBigDecimal(text));
            }
        });
        binder.registerCustomEditor(Long.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parseLong(text));
            }
        });
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parseLocalDateTime(text));
            }
        });
    }

    @GetMapping("/view")
    public String view() {
        return "company";
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String code,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String marketCategory,
            @RequestParam(required = false) BigDecimal minPeRatio,
            @RequestParam(required = false) BigDecimal maxPeRatio,
            Model model) {
        model.addAttribute("companies",
                companyService.findListRows(code, sector, marketCategory, minPeRatio, maxPeRatio));
        model.addAttribute("code", code);
        model.addAttribute("sector", sector);
        model.addAttribute("marketCategory", marketCategory);
        model.addAttribute("minPeRatio", minPeRatio);
        model.addAttribute("maxPeRatio", maxPeRatio);
        return "company-list";
    }

    @GetMapping("/{tradingCode}/report")
    public String report(@PathVariable String tradingCode, Model model, RedirectAttributes redirectAttributes) {
        try {
            Company company = companyService.findByTradingCode(tradingCode);
            EpsHistory latestEps = companyService.latestEps(company.getEpsHistory());
            DividendHistory latestDividend = companyService.latestDividend(company.getDividends());
            model.addAttribute("company", company);
            model.addAttribute("latestEpsYear", latestEps != null ? latestEps.getYear() : null);
            model.addAttribute("latestEpsDisplay", latestEpsDisplay(latestEps));
            model.addAttribute("latestDividendYear", latestDividend != null ? latestDividend.getYear() : null);
            model.addAttribute("latestDividendDisplay", latestDividendDisplay(latestDividend));
            return "company-report";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/company/list";
        }
    }

    @RequestMapping("/company-by-code")
    public String getCompanyByCode(@RequestParam String tradingCode, Model model) {
        try {
            CompanyFundamentalDto company = scraperService.scrapeCompany(tradingCode.toUpperCase().trim());
            model.addAttribute("company", company);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "company";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Company company, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes, Principal principal) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("company", company);
            model.addAttribute("error", "Please review the numeric fields and try saving again.");
            return "company";
        }

        try {
            String scrapingUserName = principal != null ? principal.getName() : null;
            Company savedCompany = companyService.save(company, scrapingUserName);
            redirectAttributes.addFlashAttribute("success",
                    "Company information for " + savedCompany.getTradingCode() + " has been saved successfully.");
            return "redirect:/company/company-by-code?tradingCode=" + savedCompany.getTradingCode();
        } catch (Exception e) {
            model.addAttribute("company", company);
            model.addAttribute("error", e.getMessage());
            return "company";
        }
    }

    private BigDecimal parseBigDecimal(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace(",", "").replace("%", "").trim();
        if (normalized.isBlank() || "-".equals(normalized) || "N/A".equalsIgnoreCase(normalized)) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    private Long parseLong(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace(",", "").trim();
        if (normalized.isBlank() || "-".equals(normalized) || "N/A".equalsIgnoreCase(normalized)) {
            return null;
        }
        return Long.valueOf(normalized);
    }

    private LocalDateTime parseLocalDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(text.trim());
    }

    private String latestEpsDisplay(EpsHistory latestEps) {
        if (latestEps == null) {
            return "-";
        }
        return latestEps.getEps() != null ? latestEps.getEps().toPlainString() : "0";
    }

    private String latestDividendDisplay(DividendHistory latestDividend) {
        if (latestDividend == null) {
            return "-";
        }
        String cash = latestDividend.getCashDividend() != null && !latestDividend.getCashDividend().isBlank()
                ? latestDividend.getCashDividend()
                : "0";
        String stock = latestDividend.getStockDividend() != null && !latestDividend.getStockDividend().isBlank()
                ? latestDividend.getStockDividend()
                : "0";
        return "Cash " + cash + "%, Stock " + stock + "%";
    }
}
