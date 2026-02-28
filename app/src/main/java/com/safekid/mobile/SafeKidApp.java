package com.safekid.mobile;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;

import com.google.android.gms.ads.MobileAds;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Hilt Application class.
 * WorkManager'ı manuel başlatarak HiltWorkerFactory'yi kullanmasını sağlar.
 * Bu sayede @HiltWorker ile işaretli Worker sınıfları Hilt injection'ı alabilir.
 */
@HiltAndroidApp
public class SafeKidApp extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        MobileAds.initialize(this, initializationStatus -> {});
    }

    /**
     * Bildirim kanallarını uygulama başlangıcında oluştur.
     * FCM arka planda bildirim gösterirken bu kanalı kullanır;
     * kanal yoksa Android 8+ bildirimi sessizce siler.
     */
    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel("geofence_breach") == null) {
            NotificationChannel channel = new NotificationChannel(
                    "geofence_breach",
                    "Güvenli Bölge Uyarıları",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Çocuğun güvenli bölge dışına çıktığında bildirim gönderilir.");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }
        if (nm.getNotificationChannel("anomaly_alerts") == null) {
            NotificationChannel channel = new NotificationChannel(
                    "anomaly_alerts",
                    "Anomali Uyarıları",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Çocuğunuzda anormal davranış tespit edildiğinde bildirim gönderilir.");
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
