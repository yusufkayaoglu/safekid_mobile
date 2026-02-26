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
import com.safekid.mobile.network.dto.FcmTokenRequest;
import com.safekid.mobile.network.dto.GeofenceAlertDto;
import com.safekid.mobile.network.dto.GeofenceAlertUnreadCountDto;
import com.safekid.mobile.network.dto.GeofenceDto;
import com.safekid.mobile.network.dto.LocationDto;
import com.safekid.mobile.network.dto.MapChildDto;
import com.safekid.mobile.network.dto.RoutePredictionResponse;
import com.safekid.mobile.network.dto.SaveGeofenceRequest;
import com.safekid.mobile.network.dto.SubscriptionStatusDto;
import com.safekid.mobile.network.dto.VerifyPurchaseRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ParentApi {

    // ── Subscription ──────────────────────────────────────────────────────────

    /** Mevcut abonelik durumunu getir */
    @GET("parent/subscription/status")
    Call<SubscriptionStatusDto> getSubscriptionStatus();

    /** Google Play satın alma token'ını backend'e doğrulat ve aktive et */
    @POST("parent/subscription/verify-purchase")
    Call<SubscriptionStatusDto> verifyPurchase(@Body VerifyPurchaseRequest request);

    // ── FCM Token ─────────────────────────────────────────────────────────────

    @PUT("parent/children/fcm-token")
    Call<Void> updateFcmToken(@Body FcmTokenRequest request);

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
    Call<Void> acknowledgeAlert(@Path("alertId") long alertId);

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

    // ── Geofence Alerts ───────────────────────────────────────────────────────

    /** Tüm geofence ihlal geçmişini getir; sadeceokunmamis=true ile sadece okunmayanlar */
    @GET("parent/geofence/alerts")
    Call<List<GeofenceAlertDto>> getGeofenceAlerts(
            @Query("sadeceokunmamis") boolean sadeceokunmamis);

    /** Okunmamış geofence alert sayısı — Response: { "okunmamis": 3 } */
    @GET("parent/geofence/alerts/unread-count")
    Call<GeofenceAlertUnreadCountDto> getGeofenceAlertUnreadCount();

    /** Belirli bir geofence alertı okundu olarak işaretle */
    @PUT("parent/geofence/alerts/{alertId}/read")
    Call<Void> markGeofenceAlertRead(@Path("alertId") long alertId);

    /** Tüm geofence alertları okundu olarak işaretle */
    @PUT("parent/geofence/alerts/read-all")
    Call<Void> markAllGeofenceAlertsRead();
}
