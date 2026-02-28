package com.safekid.mobile.service;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.ParentApi;
import com.safekid.mobile.network.dto.FcmTokenRequest;
import com.safekid.mobile.session.SessionManager;

import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SafeKidFcmService extends FirebaseMessagingService {

    private static final String CHANNEL_GEOFENCE = "geofence_breach";
    private static final String CHANNEL_ANOMALY  = "anomaly_alerts";
    private static final int NOTIF_ID_BASE = 4000;
    private static final AtomicInteger notifCounter = new AtomicInteger(0);

    private SessionManager session() {
        return new SessionManager(getApplicationContext());
    }

    /**
     * FCM token yenilendiğinde çağrılır.
     * Sadece ebeveyn giriş yapmışsa yeni token backend'e gönderilir.
     * Çocuk cihazında token yenilenirse işlem yapılmaz — zaten QrLoginFragment'ta
     * ebeveyn JWT'si ile temizleme yapılmıştır.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        SessionManager session = session();
        session.saveFcmToken(token);
        String jwtToken = session.getToken();
        if (jwtToken != null && session.isParent()) {
            ApiClient.getAuthInstance(jwtToken)
                    .create(ParentApi.class)
                    .updateFcmToken(new FcmTokenRequest(token))
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call,
                                               @NonNull Response<Void> response) {
                            android.util.Log.d("FCM", "Ebeveyn FCM token güncellendi");
                        }
                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            android.util.Log.e("FCM", "Token gönderilemedi: " + t.getMessage());
                        }
                    });
        }
    }

    /**
     * Uygulama FOREGROUND'dayken FCM mesajı geldiğinde çağrılır.
     * Background/kapalı durumda FCM SDK bildirimi otomatik gösterir.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        // Sadece ebeveyn rolündeki cihazda bildirim göster
        if (!session().isParent()) return;

        String title;
        String body;

        String type = message.getData().get("type");

        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body  = message.getNotification().getBody();
        } else {
            title = message.getData().get("title");
            body  = message.getData().get("body");
        }

        String channel = "anomaly_alert".equals(type) ? CHANNEL_ANOMALY : CHANNEL_GEOFENCE;
        showNotification(title, body, channel);
    }

    private void showNotification(String title, String body, String channel) {
        NotificationManager nm = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        int color = CHANNEL_ANOMALY.equals(channel) ? Color.YELLOW : Color.RED;

        androidx.core.app.NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, channel)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(title != null ? title : "SafeKid")
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setColor(color);

        nm.notify(NOTIF_ID_BASE + notifCounter.incrementAndGet(), builder.build());
    }
}
