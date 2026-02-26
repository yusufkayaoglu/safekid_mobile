package com.safekid.mobile.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.safekid.mobile.session.SessionManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.safekid.mobile.R;
import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.ChildApi;
import com.safekid.mobile.network.dto.LocationDto;
import com.safekid.mobile.network.dto.SendLocationRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationService extends Service {

    public static final String EXTRA_TOKEN = "token";
    private static final String CHANNEL_ID  = "safekid_location";
    private static final int    NOTIF_ID    = 1001;
    private static final long   INTERVAL_MS = 10_000;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private ChildApi childApi;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String token = intent != null ? intent.getStringExtra(EXTRA_TOKEN) : null;
        if (token == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        childApi = ApiClient.getAuthInstance(token).create(ChildApi.class);

        startForeground(NOTIF_ID, buildNotification());
        startLocationUpdates();

        return START_REDELIVER_INTENT;
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "ACCESS_FINE_LOCATION izni yok, servis duruyor");
            stopSelf();
            return;
        }

        Log.d("LocationService", "Konum güncellemeleri başlatılıyor...");

        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }

        // Son bilinen konumu hemen gönder
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.d("LocationService", "Son bilinen konum alındı, hemen gönderiliyor");
                sendLocationToServer(location);
            } else {
                Log.w("LocationService", "Son bilinen konum null, periyodik güncelleme bekleniyor");
            }
        });

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(INTERVAL_MS / 2)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                List<Location> locations = result.getLocations();
                if (!locations.isEmpty()) {
                    sendLocationToServer(locations.get(locations.size() - 1));
                }
            }
        };

        fusedClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void sendLocationToServer(Location location) {
        String recordedAt = iso8601Now();
        SendLocationRequest req = new SendLocationRequest(
                location.getLatitude(),
                location.getLongitude(),
                recordedAt
        );

        childApi.sendLocation(req).enqueue(new Callback<LocationDto>() {
            @Override
            public void onResponse(@NonNull Call<LocationDto> call,
                                   @NonNull Response<LocationDto> response) {
                if (response.isSuccessful()) {
                    Log.d("LocationService", "Konum gönderildi: " + req.lat + ", " + req.lng);
                    new SessionManager(LocationService.this)
                            .saveLastLocationSentAt(System.currentTimeMillis());
                } else {
                    Log.e("LocationService", "Sunucu hatası: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<LocationDto> call, @NonNull Throwable t) {
                Log.e("LocationService", "Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.notification_channel_desc));
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private String iso8601Now() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    @Override
    public void onDestroy() {
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}