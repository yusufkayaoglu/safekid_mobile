package com.safekid.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * POST /parent/geofence gövdesi.
 * koordinatlar → [[lng, lat], [lng, lat], ...] — Google Maps'ten alırken
 * önce .longitude, sonra .latitude alınır (GeoJSON standartı).
 */
public class SaveGeofenceRequest {

    @SerializedName("cocukId")
    public String cocukId;

    @SerializedName("alanAdi")
    public String alanAdi;

    /** Her eleman [longitude, latitude] ikilisi */
    @SerializedName("koordinatlar")
    public List<List<Double>> koordinatlar;

    public SaveGeofenceRequest(String cocukId, String alanAdi,
                               List<List<Double>> koordinatlar) {
        this.cocukId = cocukId;
        this.alanAdi = alanAdi;
        this.koordinatlar = koordinatlar;
    }
}
