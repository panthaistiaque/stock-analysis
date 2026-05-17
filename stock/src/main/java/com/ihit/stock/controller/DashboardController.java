package com.ihit.stock.controller;

import com.ihit.stock.dto.MissingMarketDataDto;
import com.ihit.stock.dto.CompanyListRow;
import com.ihit.stock.dto.SectorCoverageDto;
import com.ihit.stock.dto.SmartAlertDto;
import com.ihit.stock.service.AppUserService;
import com.ihit.stock.service.CompanyService;
import com.ihit.stock.service.DashboardService;
import com.ihit.stock.repository.TradingCodeRepository;
import com.ihit.stock.service.TradingCodeService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final AppUserService userService;
    private final DashboardService dashboardService;
    private final TradingCodeService tradingCodeService;
    private final CompanyService companyService;

    public DashboardController(AppUserService userService, DashboardService dashboardService,
            TradingCodeService tradingCodeService, CompanyService companyService) {
        this.userService = userService;
        this.dashboardService = dashboardService;
        this.tradingCodeService = tradingCodeService;
        this.companyService = companyService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        boolean isAdmin = userService.isAdmin(authentication);
        model.addAttribute("isAdmin", isAdmin);

        // Fetch missing market data
        List<TradingCodeRepository.TradingCodeProjection> allDetails = tradingCodeService.getAllTradingCodes();
        model.addAttribute("totalCompanies", allDetails.size());
        model.addAttribute("allCompanyDetails", allDetails);

        List<CompanyListRow> fundamentalDetails = companyService.findAllListRows();
        long totalFundamentalDone = dashboardService.countFundamentalCompanyRows();
        model.addAttribute("totalFundamentalDone", totalFundamentalDone);
        model.addAttribute("fundamentalDetails", fundamentalDetails);
        long missingFundamentalCount = Math.max(0, allDetails.size() - totalFundamentalDone);
        model.addAttribute("missingFundamentalCount", missingFundamentalCount);
        model.addAttribute("fundamentalCoveragePercent",
                dashboardService.calculateCoveragePercent((int) totalFundamentalDone, allDetails.size()));

        List<String> tradingCodes = allDetails.stream().map(TradingCodeRepository.TradingCodeProjection::getCode).collect(Collectors.toList());

        List<MissingMarketDataDto> missingDataList = dashboardService.findMissingMarketData(tradingCodes);
        model.addAttribute("missingDataCount", missingDataList.size());
        model.addAttribute("missingDataList", missingDataList);
        int marketDataCompanyCount = dashboardService.countTradingCodesWithMarketData();
        model.addAttribute("marketDataCompanyCount", marketDataCompanyCount);
        model.addAttribute("marketCoveragePercent",
                dashboardService.calculateCoveragePercent(marketDataCompanyCount, allDetails.size()));
        model.addAttribute("marketCoverageSummary", dashboardService.findMarketCoverageSummary());
        
        model.addAttribute("marketStatusMessage", dashboardService.getMarketStatusMessage());
        model.addAttribute("missingMarketDates", dashboardService.getMissingMarketDates());
        model.addAttribute("syncRequired", dashboardService.isSyncRequired());
        model.addAttribute("latestMarketDate", dashboardService.getLatestMarketDate());
        model.addAttribute("marketRecordCount", dashboardService.countMarketRecords());
        model.addAttribute("savedForecastCount", dashboardService.countSavedForecasts());
        model.addAttribute("verifiedForecastCount", dashboardService.countVerifiedForecasts());
        model.addAttribute("forecastCoverageSummary", dashboardService.findForecastCoverageSummary());

        List<SectorCoverageDto> sectorCoverage = dashboardService.findSectorCoverage();
        model.addAttribute("sectorCoverage", sectorCoverage);
        model.addAttribute("sectorCoveragePreview", sectorCoverage.stream().limit(4).collect(Collectors.toList()));
        model.addAttribute("sectorCount", sectorCoverage.size());

        List<SmartAlertDto> smartAlerts = dashboardService.findSmartAlerts();
        model.addAttribute("smartAlerts", smartAlerts);
        model.addAttribute("smartAlertPreview", smartAlerts.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("smartAlertCount", smartAlerts.size());

        // Add other dashboard attributes as needed (e.g., appTitle, username, headerMenus, user, sidebarMenus)
        // These are typically handled by a base controller or interceptor in a real application.
        return "dashboard";
    }
}
