package com.safekid.mobile.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.hilt.work.HiltWorker;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Tasks;
import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.ChildApi;
import com.safekid.mobile.network.dto.LocationDto;
import com.safekid.mobile.network.dto.SendLocationRequest;
import com.safekid.mobile.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import retrofit2.Response;

/**
 * WorkManager Worker — çocuğun konumunu 30 saniyede bir backend'e gönderir.
 *
 * ⚠️  WorkManager.PeriodicWorkRequest minimum aralığı 15 dakikadır (Android kısıtı).
 *     30 saniyelik güncelleme için bu Worker kendini tamamlandıktan 30 sn sonra
 *     yeniden planlar (OneTimeWorkRequest zinciri).
 *
 * Hilt ile entegre → @HiltWorker + @AssistedInject
 * SafeKidApp.getWorkManagerConfiguration() HiltWorkerFactory kullanır.
 *
 * Başlatmak için:
 *   LocationWorker.enqueueNext(context);
 *
 * Durdurmak için:
 *   WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
 */
@HiltWorker
public class LocationWorker extends Worker {

    public static final String WORK_NAME = "location_update_worker";
    private static final String TAG      = "LocationWorker";
    private static final long   INTERVAL_SEC = 10L;

    private final SessionManager sessionManager;

    @AssistedInject
    public LocationWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            SessionManager sessionManager) {
        super(context, params);
        this.sessionManager = sessionManager;
    }

    // ── Work Logic ────────────────────────────────────────────────────────────

    @NonNull
    @Override
    public Result doWork() {
        String token = sessionManager.getToken();
        if (token == null || !sessionManager.isChild()) {
            Log.w(TAG, "Token yok veya çocuk değil, iş iptal edildi.");
            return Result.failure();
        }

        if (!hasLocationPermission()) {
            Log.e(TAG, "Konum izni yok.");
            return Result.failure();
        }

        try {
            fetchAndSendLocation(token);
        } catch (Exception e) {
            Log.e(TAG, "Beklenmeyen hata: " + e.getMessage());
        } finally {
            // Başarısız da olsa 30 sn sonra tekrar çalıştır.
            // Result.retry() kullanmıyoruz — WorkManager'ın kendi backoff
            // mekanizması enqueueNext() ile çakışır.
            enqueueNext(getApplicationContext());
        }
        return Result.success();
    }

    /**
     * FusedLocationProviderClient ile mevcut konumu alır, backend'e gönderir.
     * Tasks.await() → Worker background thread'de çalıştığından güvenli.
     */
    @SuppressLint("MissingPermission")
    private void fetchAndSendLocation(String token) throws Exception {
        FusedLocationProviderClient fusedClient =
                LocationServices.getFusedLocationProviderClient(getApplicationContext());

        android.location.Location location;
        try {
            // Önce yüksek doğruluklu güncel konum dene
            location = Tasks.await(
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                    15, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.w(TAG, "getCurrentLocation başarısız, getLastLocation deneniyor.");
            location = Tasks.await(fusedClient.getLastLocation(), 10, TimeUnit.SECONDS);
        }

        if (location == null) {
            Log.w(TAG, "Konum null döndü, bu çevrim atlandı.");
            return;
        }

        sendToBackend(token, location.getLatitude(), location.getLongitude());
    }

    /** Senkron Retrofit çağrısı ile konumu backend'e gönderir */
    private void sendToBackend(String token, double lat, double lng) {
        try {
            ChildApi childApi = ApiClient.getAuthInstance(token).create(ChildApi.class);
            SendLocationRequest req = new SendLocationRequest(lat, lng, iso8601Now());
            Response<LocationDto> response = childApi.sendLocation(req).execute();

            if (response.isSuccessful()) {
                Log.d(TAG, "Konum gönderildi: " + lat + ", " + lng);
            } else {
                Log.e(TAG, "Sunucu hatası: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Ağ hatası: " + e.getMessage());
        }
    }

    // ── Static Helpers ────────────────────────────────────────────────────────

    /**
     * 30 saniye sonra bir sonraki LocationWorker çalıştırmasını planlar.
     * KEEP politikası: zaten planlanmış iş varsa bekletilir, iki kez çalışmaz.
     */
    public static void enqueueNext(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LocationWorker.class)
                .setInitialDelay(INTERVAL_SEC, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }

    /**
     * İlk kez başlatmak için (gecikme olmadan).
     * Zaten çalışan bir iş varsa yenisini REPLACE eder.
     */
    public static void startNow(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LocationWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    /** Worker zincirini durdurur */
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "LocationWorker zinciri iptal edildi.");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static String iso8601Now() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
