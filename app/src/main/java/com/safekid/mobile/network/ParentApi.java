package com.safekid.mobile.network;

import com.safekid.mobile.network.dto.AddChildRequest;
import com.safekid.mobile.network.dto.AiChatRequest;
import com.safekid.mobile.network.dto.AiChatResponse;
import com.safekid.mobile.network.dto.AlertDto;
import com.safekid.mobile.network.dto.AnomalyCheckResponse;
import com.safekid.mobile.network.dto.ChildDto;
import com.safekid.mobile.network.dto.CocukIdRequest;
import com.safekid.mobile.network.dto.DailySummaryRequest;
import com.safekid.mobile.network.dto.DailySummaryResponse;
import com.safekid.mobile.network.dto.GeofenceDto;
import com.safekid.mobile.network.dto.LocationDto;
import com.safekid.mobile.network.dto.MapChildDto;
import com.safekid.mobile.network.dto.RoutePredictionResponse;
import com.safekid.mobile.network.dto.SaveGeofenceRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ParentApi {

    // ── Child Management ──────────────────────────────────────────────────────

    @POST("parent/children")
    Call<ChildDto> addChild(@Body AddChildRequest request);

    @GET("parent/children")
    Call<List<ChildDto>> getChildren();

    @GET("parent/children/{childId}")
    Call<ChildDto> getChild(@Path("childId") String childId);

    @DELETE("parent/children/{childId}")
    Call<Void> deleteChild(@Path("childId") String childId);

    // ── Location ──────────────────────────────────────────────────────────────

    @GET("parent/children/{childId}/last-location")
    Call<LocationDto> getLastLocation(@Path("childId") String childId);

    @GET("parent/children/map")
    Call<List<MapChildDto>> getMapChildren();

    // ── AI Features ───────────────────────────────────────────────────────────

    @POST("parent/ai/route-prediction")
    Call<RoutePredictionResponse> routePrediction(@Body CocukIdRequest request);

    @POST("parent/ai/anomaly-check")
    Call<AnomalyCheckResponse> anomalyCheck(@Body CocukIdRequest request);

    @POST("parent/ai/daily-summary")
    Call<DailySummaryResponse> dailySummary(@Body DailySummaryRequest request);

    @POST("parent/ai/chat")
    Call<AiChatResponse> aiChat(@Body AiChatRequest request);

    @GET("parent/ai/alerts")
    Call<List<AlertDto>> getAlerts();

    @PUT("parent/ai/alerts/{alertId}/acknowledge")
    Call<Void> acknowledgeAlert(@Path("alertId") String alertId);

    // ── Geofence ──────────────────────────────────────────────────────────────

    /** Yeni güvenli bölge oluştur */
    @POST("parent/geofence")
    Call<GeofenceDto> createGeofence(@Body SaveGeofenceRequest request);

    /** Bir çocuğun tüm aktif güvenli bölgelerini getir */
    @GET("parent/geofence/{childId}")
    Call<List<GeofenceDto>> getGeofences(@Path("childId") String childId);

    /** Güvenli bölge güncelle */
    @PUT("parent/geofence/{geofenceId}")
    Call<GeofenceDto> updateGeofence(@Path("geofenceId") long geofenceId,
                                     @Body SaveGeofenceRequest request);

    /** Güvenli bölge sil */
    @DELETE("parent/geofence/{geofenceId}")
    Call<Void> deleteGeofence(@Path("geofenceId") long geofenceId);
}
