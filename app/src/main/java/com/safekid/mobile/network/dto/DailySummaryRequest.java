package com.safekid.mobile.network.dto;

public class DailySummaryRequest {
    public String cocukUniqueId;
    public String date; // optional, format: YYYY-MM-DD

    public DailySummaryRequest(String cocukUniqueId, String date) {
        this.cocukUniqueId = cocukUniqueId;
        this.date = date;
    }
}
