package com.ihit.stock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/documentation")
public class DocumentationController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Knowledge Base");
        return "admin/documentation";
    }

    @GetMapping("/fundamental-analysis")
    public String fundamentalAnalysis(Model model) {
        model.addAttribute("pageTitle", "Fundamental Analysis Guide");
        return "admin/fundamental-analysis";
    }

    @GetMapping("/technical-indicators")
    public String technicalIndicators(Model model) {
        model.addAttribute("pageTitle", "Technical Indicators Guide");
        return "admin/documentation/technical-indicators";
    }
}