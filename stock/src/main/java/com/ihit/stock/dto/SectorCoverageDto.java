package com.ihit.stock.dto;

public class SectorCoverageDto {

    private final String sector;
    private final long companyCount;

    public SectorCoverageDto(String sector, long companyCount) {
        this.sector = sector;
        this.companyCount = companyCount;
    }

    public String getSector() {
        return sector;
    }

    public long getCompanyCount() {
        return companyCount;
    }
}
