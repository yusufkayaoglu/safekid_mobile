package com.safekid.mobile.network.dto;

public class SendLocationRequest {
    public double lat;
    public double lng;
    public String recordedAt;

    public SendLocationRequest(double lat, double lng, String recordedAt) {
        this.lat = lat;
        this.lng = lng;
        this.recordedAt = recordedAt;
    }
}
