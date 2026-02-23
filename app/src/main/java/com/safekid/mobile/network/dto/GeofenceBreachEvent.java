package com.safekid.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * SSE "geofence-breach" event verisi.
 *
 * Örnek:
 * event: geofence-breach
 * data: {
 *   "type": "GEOFENCE_BREACH",
 *   "cocukId": "01HXYZ...",
 *   "cocukAdi": "Ali",
 *   "lat": 41.015,
 *   "lng": 28.979,
 *   "geofenceId": 7,
 *   "alanAdi": "Okul Bölgesi",
 *   "zaman": "2026-02-23T10:05:00Z"
 * }
 */
public class GeofenceBreachEvent {

    @SerializedName("type")
    public String type;       // "GEOFENCE_BREACH"

    @SerializedName("cocukId")
    public String cocukId;

    @SerializedName("cocukAdi")
    public String cocukAdi;

    @SerializedName("lat")
    public double lat;

    @SerializedName("lng")
    public double lng;

    @SerializedName("geofenceId")
    public long geofenceId;

    @SerializedName("alanAdi")
    public String alanAdi;

    @SerializedName("zaman")
    public String zaman;
}
