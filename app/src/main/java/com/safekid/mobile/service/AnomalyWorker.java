package com.safekid.mobile.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.hilt.work.HiltWorker;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.safekid.mobile.MainActivity;
import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.ParentApi;
import com.safekid.mobile.network.dto.AnomalyCheckResponse;
import com.safekid.mobile.network.dto.ChildDto;
import com.safekid.mobile.network.dto.CocukIdRequest;
import com.safekid.mobile.session.SessionManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import retrofit2.Response;

/**
 * WorkManager PeriodicWorker — 15 dakikada bir tüm çocuklar için anomali kontrolü yapar.
 * Sadece premium ebeveyn hesaplarında çalışır.
 *
 * Başlatmak için: AnomalyWorker.start(context)
 * Durdurmak için: AnomalyWorker.cancel(context)
 */
@HiltWorker
public class AnomalyWorker extends Worker {

    public static final String WORK_NAME = "anomaly_check_worker";
    private static final String TAG = "AnomalyWorker";
    private static final String CHANNEL_ID = "anomaly_alerts";
    private static final int BASE_NOTIFICATION_ID = 5000;

    private final SessionManager sessionManager;

    @AssistedInject
    public AnomalyWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            SessionManager sessionManager) {
        super(context, params);
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!sessionManager.isParent() || !sessionManager.isPremium()) {
            Log.d(TAG, "Ebeveyn değil veya premium değil, iş atlandı.");
            return Result.success();
        }

        String token = sessionManager.getToken();
        if (token == null) {
            Log.w(TAG, "Token bulunamadı, iş atlandı.");
            return Result.success();
        }

        try {
            ParentApi parentApi = ApiClient.getAuthInstance(token).create(ParentApi.class);

            Response<List<ChildDto>> childrenResponse = parentApi.getChildren().execute();
            if (!childrenResponse.isSuccessful() || childrenResponse.body() == null) {
                Log.w(TAG, "Çocuk listesi alınamadı: " + childrenResponse.code());
                return Result.success();
            }

            List<ChildDto> children = childrenResponse.body();
            for (int i = 0; i < children.size(); i++) {
                ChildDto child = children.get(i);
                checkAnomaly(parentApi, child, i);
            }
        } catch (Exception e) {
            Log.e(TAG, "Anomali kontrolü sırasında hata: " + e.getMessage());
        }

        return Result.success();
    }

    private void checkAnomaly(ParentApi parentApi, ChildDto child, int index) {
        try {
            Response<AnomalyCheckResponse> response =
                    parentApi.anomalyCheck(new CocukIdRequest(child.cocukUniqueId)).execute();

            if (response.isSuccessful() && response.body() != null
                    && response.body().anomalyDetected) {
                AnomalyCheckResponse result = response.body();
                String childName = child.getFullName().trim();
                String summary = result.summary != null ? result.summary : "Anormal davranış tespit edildi.";
                sendNotification(BASE_NOTIFICATION_ID + index, childName, summary);
                Log.d(TAG, "Anomali tespit edildi: " + childName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Çocuk anomali kontrolü hatası (" + child.cocukUniqueId + "): " + e.getMessage());
        }
    }

    private void sendNotification(int notifId, String childName, String summary) {
        Context context = getApplicationContext();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ Anomali: " + childName)
                .setContentText(summary)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(summary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notifId, builder.build());
    }

    // ── Static Helpers ────────────────────────────────────────────────────────

    /** 15 dakikada bir çalışacak periyodik görevi başlatır (zaten varsa mevcut zamanlamayı korur). */
    public static void start(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(AnomalyWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request);

        Log.d(TAG, "AnomalyWorker başlatıldı (KEEP policy).");
    }

    /** Periyodik görevi iptal eder. */
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "AnomalyWorker iptal edildi.");
    }
}
