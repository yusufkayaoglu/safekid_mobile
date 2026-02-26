package com.safekid.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

public class AlertDto {
    @SerializedName("id")
    public long alertId;
    public String cocukUniqueId;
    public String cocukAdi;
    public String analysisType;
    public String resultJson;
    public String createdAt;
}
