package com.safekid.mobile.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.ParentApi;
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
import com.safekid.mobile.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentViewModel extends AndroidViewModel {

    private ParentApi parentApi;
    private final SessionManager session;

    private final MutableLiveData<List<ChildDto>> children = new MutableLiveData<>();
    private final MutableLiveData<List<MapChildDto>> mapChildren = new MutableLiveData<>();
    private final MutableLiveData<ChildDto> addedChild = new MutableLiveData<>();
    private final MutableLiveData<LocationDto> lastLocation = new MutableLiveData<>();
    private final MutableLiveData<AiChatResponse> chatReply = new MutableLiveData<>();
    private final MutableLiveData<List<AlertDto>> alerts = new MutableLiveData<>();
    private final MutableLiveData<RoutePredictionResponse> routePrediction = new MutableLiveData<>();
    private final MutableLiveData<AnomalyCheckResponse> anomalyCheck = new MutableLiveData<>();
    private final MutableLiveData<DailySummaryResponse> dailySummary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    // ── Geofence ──────────────────────────────────────────────────────────────
    private final MutableLiveData<List<GeofenceDto>> geofences = new MutableLiveData<>();
    private final MutableLiveData<GeofenceDto> savedGeofence = new MutableLiveData<>();

    public ParentViewModel(@NonNull Application application) {
        super(application);
        session = new SessionManager(application);
        parentApi = ApiClient.getAuthInstance(session.getToken()).create(ParentApi.class);
    }

    /** Token değiştiğinde (logout/login) çağrılır — parentApi güncel token ile yenilenir */
    public void refreshApiClient() {
        parentApi = ApiClient.getAuthInstance(session.getToken()).create(ParentApi.class);
    }

    // ── Exposed LiveData ──────────────────────────────────────────────────────

    public LiveData<List<ChildDto>> getChildren() { return children; }
    public LiveData<List<MapChildDto>> getMapChildren() { return mapChildren; }
    public LiveData<ChildDto> getAddedChild() { return addedChild; }
    public void clearAddedChild() { addedChild.setValue(null); }
    public LiveData<LocationDto> getLastLocation() { return lastLocation; }
    public void clearLastLocation() { lastLocation.setValue(null); }
    public LiveData<AiChatResponse> getChatReply() { return chatReply; }
    public LiveData<List<AlertDto>> getAlerts() { return alerts; }
    public LiveData<RoutePredictionResponse> getRoutePrediction() { return routePrediction; }
    public LiveData<AnomalyCheckResponse> getAnomalyCheck() { return anomalyCheck; }
    public LiveData<DailySummaryResponse> getDailySummary() { return dailySummary; }
    public LiveData<Boolean> getDeleteSuccess() { return deleteSuccess; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> isLoading() { return loading; }

    // Geofence LiveData
    public LiveData<List<GeofenceDto>> getGeofences() { return geofences; }
    public LiveData<GeofenceDto> getSavedGeofence() { return savedGeofence; }
    public void clearSavedGeofence() { savedGeofence.setValue(null); }

    // ── Children ──────────────────────────────────────────────────────────────

    public void loadChildren() {
        loading.setValue(true);
        parentApi.getChildren().enqueue(new Callback<List<ChildDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChildDto>> call,
                                   @NonNull Response<List<ChildDto>> response) {
                loading.postValue(false);
                if (response.isSuccessful()) {
                    children.postValue(response.body());
                } else {
                    error.postValue("Çocuklar yüklenemedi: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChildDto>> call, @NonNull Throwable t) {
                loading.postValue(false);
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    public void addChild(String adi, String soyadi, String telefon, String mail) {
        loading.setValue(true);
        parentApi.addChild(new AddChildRequest(adi, soyadi, telefon, mail))
                .enqueue(new Callback<ChildDto>() {
                    @Override
                    public void onResponse(@NonNull Call<ChildDto> call,
                                           @NonNull Response<ChildDto> response) {
                        loading.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            addedChild.postValue(response.body());
                        } else {
                            error.postValue("Çocuk eklenemedi: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ChildDto> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    public void deleteChild(String childId) {
        loading.setValue(true);
        parentApi.deleteChild(childId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                loading.postValue(false);
                if (response.isSuccessful()) {
                    deleteSuccess.postValue(true);
                } else {
                    error.postValue("Silme başarısız: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                loading.postValue(false);
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    // ── Location ──────────────────────────────────────────────────────────────

    public void loadLastLocation(String childId) {
        parentApi.getLastLocation(childId).enqueue(new Callback<LocationDto>() {
            @Override
            public void onResponse(@NonNull Call<LocationDto> call,
                                   @NonNull Response<LocationDto> response) {
                if (response.isSuccessful()) {
                    lastLocation.postValue(response.body());
                } else if (response.code() == 404) {
                    lastLocation.postValue(null); // Henüz konum kaydı yok
                } else {
                    error.postValue("Konum alınamadı: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<LocationDto> call, @NonNull Throwable t) {
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    public void loadMapChildren() {
        parentApi.getMapChildren().enqueue(new Callback<List<MapChildDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MapChildDto>> call,
                                   @NonNull Response<List<MapChildDto>> response) {
                if (response.isSuccessful()) {
                    mapChildren.postValue(response.body());
                } else {
                    error.postValue("Harita verisi alınamadı: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MapChildDto>> call, @NonNull Throwable t) {
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    // ── AI Chat ───────────────────────────────────────────────────────────────

    public void sendMessage(String cocukId, String message) {
        loading.setValue(true);
        parentApi.aiChat(new AiChatRequest(cocukId, message))
                .enqueue(new Callback<AiChatResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AiChatResponse> call,
                                           @NonNull Response<AiChatResponse> response) {
                        loading.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            chatReply.postValue(response.body());
                        } else {
                            error.postValue("AI yanıt veremedi: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<AiChatResponse> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    // ── AI Alerts ─────────────────────────────────────────────────────────────

    public void loadAlerts() {
        loading.setValue(true);
        parentApi.getAlerts().enqueue(new Callback<List<AlertDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AlertDto>> call,
                                   @NonNull Response<List<AlertDto>> response) {
                loading.postValue(false);
                if (response.isSuccessful()) {
                    alerts.postValue(response.body());
                } else {
                    error.postValue("Uyarılar alınamadı: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AlertDto>> call, @NonNull Throwable t) {
                loading.postValue(false);
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    public void acknowledgeAlert(String alertId) {
        parentApi.acknowledgeAlert(alertId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    loadAlerts(); // refresh
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    // ── AI Analysis ───────────────────────────────────────────────────────────

    public void runRoutePrediction(String cocukId) {
        loading.setValue(true);
        parentApi.routePrediction(new CocukIdRequest(cocukId))
                .enqueue(new Callback<RoutePredictionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RoutePredictionResponse> call,
                                           @NonNull Response<RoutePredictionResponse> response) {
                        loading.postValue(false);
                        if (response.isSuccessful()) routePrediction.postValue(response.body());
                        else error.postValue("Rota tahmini başarısız: " + response.code());
                    }

                    @Override
                    public void onFailure(@NonNull Call<RoutePredictionResponse> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    public void runAnomalyCheck(String cocukId) {
        loading.setValue(true);
        parentApi.anomalyCheck(new CocukIdRequest(cocukId))
                .enqueue(new Callback<AnomalyCheckResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AnomalyCheckResponse> call,
                                           @NonNull Response<AnomalyCheckResponse> response) {
                        loading.postValue(false);
                        if (response.isSuccessful()) anomalyCheck.postValue(response.body());
                        else error.postValue("Anomali kontrolü başarısız: " + response.code());
                    }

                    @Override
                    public void onFailure(@NonNull Call<AnomalyCheckResponse> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    // ── Geofence ──────────────────────────────────────────────────────────────

    public void loadGeofences(String childId) {
        parentApi.getGeofences(childId).enqueue(new Callback<List<GeofenceDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<GeofenceDto>> call,
                                   @NonNull Response<List<GeofenceDto>> response) {
                if (response.isSuccessful()) {
                    geofences.postValue(response.body());
                } else {
                    error.postValue("Güvenli bölgeler yüklenemedi: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GeofenceDto>> call, @NonNull Throwable t) {
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    public void createGeofence(String cocukId, String alanAdi,
                               List<List<Double>> koordinatlar) {
        loading.setValue(true);
        SaveGeofenceRequest req = new SaveGeofenceRequest(cocukId, alanAdi, koordinatlar);
        parentApi.createGeofence(req).enqueue(new Callback<GeofenceDto>() {
            @Override
            public void onResponse(@NonNull Call<GeofenceDto> call,
                                   @NonNull Response<GeofenceDto> response) {
                loading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    savedGeofence.postValue(response.body());
                } else {
                    error.postValue("Bölge kaydedilemedi: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeofenceDto> call, @NonNull Throwable t) {
                loading.postValue(false);
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    public void deleteGeofence(long geofenceId, String childId) {
        parentApi.deleteGeofence(geofenceId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    loadGeofences(childId); // listeyi yenile
                } else {
                    error.postValue("Bölge silinemedi: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    public void runDailySummary(String cocukId, String date) {
        loading.setValue(true);
        parentApi.dailySummary(new DailySummaryRequest(cocukId, date))
                .enqueue(new Callback<DailySummaryResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DailySummaryResponse> call,
                                           @NonNull Response<DailySummaryResponse> response) {
                        loading.postValue(false);
                        if (response.isSuccessful()) dailySummary.postValue(response.body());
                        else error.postValue("Özet alınamadı: " + response.code());
                    }

                    @Override
                    public void onFailure(@NonNull Call<DailySummaryResponse> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }
}
