package com.safekid.mobile.network.dto;

/**
 * Backend GET /parent/geofence/alerts yanıtı.
 *
 * Örnek:
 * {
 *   "id": 1,
 *   "cocukId": "01HXYZ...",
 *   "cocukAdiSoyadi": "Ali Kayaoğlu",
 *   "geofenceId": 7,
 *   "alanAdi": "Okul Bölgesi",
 *   "lat": 41.015,
 *   "lng": 28.979,
 *   "zaman": "2026-02-23T10:05:00Z",
 *   "okundu": false
 * }
 */
public class GeofenceAlertDto {
    public long id;
    public String cocukId;
    public String cocukAdiSoyadi;
    public long geofenceId;
    public String alanAdi;
    public double lat;
    public double lng;
    public String zaman;
    public boolean okundu;
}
