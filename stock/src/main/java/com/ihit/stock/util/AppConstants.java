package com.ihit.stock.util;

public final class AppConstants {
    private AppConstants() {
        throw new IllegalStateException("Utility class");
    }

    // =========================
    // MESSAGES
    // =========================
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String WARNING = "warning";
    public static final String INFO = "info";

    // =========================
    // USER / SECURITY
    // =========================

    public static final String USER = "user";
    public static final String USERNAME = "username";

    // =========================
    // STOCK FILTERS
    // =========================

    public static final String TRADING_CODE = "tradingCode";
    public static final String FROM_DATE = "fromDate";
    public static final String TO_DATE = "toDate";

    // =========================
    // PAGINATION
    // =========================

    public static final String PAGE = "page";
    public static final String SIZE = "size";
}
