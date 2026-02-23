package com.safekid.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Backend'in /parent/geofence yanıtı.
 * koordinatlar → [[lng, lat], [lng, lat], ...]  — GeoJSON sırası
 */
public class GeofenceDto {

    @SerializedName("id")
    public long id;

    @SerializedName("cocukId")
    public String cocukId;

    @SerializedName("alanAdi")
    public String alanAdi;

    /** Her eleman [longitude, latitude] ikilisi */
    @SerializedName("koordinatlar")
    public List<List<Double>> koordinatlar;

    @SerializedName("aktif")
    public boolean aktif;

    @SerializedName("olusturmaTarihi")
    public String olusturmaTarihi;
}
