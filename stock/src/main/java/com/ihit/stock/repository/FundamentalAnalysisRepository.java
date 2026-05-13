package com.ihit.stock.repository;

import com.ihit.stock.model.Company;
import org.springframework.stereotype.Repository;

@Repository
public class FundamentalAnalysisRepository {
    private final CompanyRepository companyRepository;

    public FundamentalAnalysisRepository(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company findCompany(String tradingCode) {
        if (tradingCode == null || tradingCode.isBlank()) {
            throw new IllegalArgumentException("Trading code is required.");
        }
        return companyRepository.findById(tradingCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Company data not found for " + tradingCode + "."));
    }
}
