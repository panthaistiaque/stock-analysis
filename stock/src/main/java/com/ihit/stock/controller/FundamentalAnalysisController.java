package com.ihit.stock.controller;

import com.ihit.stock.service.FundamentalAnalysisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fundamental-analysis")
public class FundamentalAnalysisController {
    private final FundamentalAnalysisService analysisService;

    public FundamentalAnalysisController(FundamentalAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/{tradingCode}")
    public String report(@PathVariable String tradingCode, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("analysis", analysisService.analyze(tradingCode));
            return "fundamental-analysis";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/company/list";
        }
    }
}
