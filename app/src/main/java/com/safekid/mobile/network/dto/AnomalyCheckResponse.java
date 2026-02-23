package com.safekid.mobile.network.dto;

import java.util.List;

public class AnomalyCheckResponse {
    public boolean anomalyDetected;
    public List<String> anomalies;
    public String summary;
}
