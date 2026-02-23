package com.safekid.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.Gson;
import com.safekid.mobile.databinding.FragmentParentMapBinding;
import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.dto.GeofenceBreachEvent;
import com.safekid.mobile.network.dto.GeofenceDto;
import com.safekid.mobile.network.dto.MapChildDto;
import com.safekid.mobile.session.SessionManager;
import com.safekid.mobile.viewmodel.ParentViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class ParentMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String CHANNEL_GEOFENCE = "geofence_breach";
    private static final int    NOTIF_ID_BASE     = 3000;

    private FragmentParentMapBinding binding;
    private ParentViewModel viewModel;
    private SessionManager session;
    private GoogleMap googleMap;

    private final Map<String, Marker> markerMap = new HashMap<>();
    // childId → polygon listesi (bir çocuğun birden fazla bölgesi olabilir)
    private final Map<String, List<Polygon>> geofencePolygons = new HashMap<>();

    private EventSource eventSource;
    private final Gson gson = new Gson();
    private final AtomicInteger notifCounter = new AtomicInteger(0);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentParentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);
        session   = new SessionManager(requireContext());

        // Back-stack'ten dönünce mevcut SupportMapFragment'ı yeniden kullan.
        // Her seferinde newInstance() çağırmak haritayı sıfırlar ve marker'ları kaybettirir.
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapView, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });

        binding.fabRefresh.setOnClickListener(v -> {
            viewModel.loadMapChildren();
            refreshAllGeofences();
        });

        binding.fabGeofence.setOnClickListener(v -> openGeofenceDrawForChild());

        createNotificationChannel();
    }

    // ── Map Ready ─────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        // findFragmentById ile aynı SupportMapFragment yeniden kullanılıyorsa
        // googleMap aynı instance'tır → markerlar hâlâ geçerli, temizleme.
        // Farklı (yeni) bir instance geldiyse eski Marker/Polygon referansları
        // geçersizdir → temizle ve haritayı sıfırla.
        boolean isNewMap = (googleMap != map);
        googleMap = map;

        if (isNewMap) {
            markerMap.clear();
            geofencePolygons.clear();
        }

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Çocuk markerlarını yükle
        viewModel.getMapChildren().observe(getViewLifecycleOwner(), this::updateMarkers);
        viewModel.loadMapChildren();

        // Geofence polygon'larını yükle (harita hazır olduktan sonra)
        viewModel.getGeofences().observe(getViewLifecycleOwner(), geofences -> {
            if (geofences == null || geofences.isEmpty()) return;
            String childId = geofences.get(0).cocukId;
            drawGeofencesForChild(childId, geofences);
        });

        // SSE (her view yaşam döngüsünde yeniden bağlan)
        startSse();
    }

    // ── Marker Yönetimi ───────────────────────────────────────────────────────

    private void updateMarkers(List<MapChildDto> children) {
        if (children == null || googleMap == null) return;
        for (MapChildDto child : children) {
            addOrUpdateMarker(child);
            // Harita açılır açılmaz bu çocuğun geofence'lerini de yükle
            viewModel.loadGeofences(child.childId);
        }
    }

    private void addOrUpdateMarker(MapChildDto child) {
        if (child == null || child.lat == null || child.lng == null || googleMap == null) return;

        LatLng pos = new LatLng(child.lat, child.lng);
        Marker marker = markerMap.get(child.childId);

        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(child.childName));
            markerMap.put(child.childId, marker);
            // İlk ekleme: kamera git
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
        } else {
            marker.setPosition(pos);
        }
    }

    // ── Geofence Çizimi ───────────────────────────────────────────────────────

    /**
     * Bir çocuğa ait geofence listesini haritada yeşil polygon olarak çizer.
     * Aynı çocuğun eski polygonları önce silinir.
     */
    private void drawGeofencesForChild(String childId, List<GeofenceDto> zones) {
        if (googleMap == null) return;

        // Eski polygon'ları kaldır
        List<Polygon> old = geofencePolygons.get(childId);
        if (old != null) {
            for (Polygon p : old) p.remove();
        }

        List<Polygon> newPolygons = new ArrayList<>();
        for (GeofenceDto zone : zones) {
            if (!zone.aktif) continue;
            if (zone.koordinatlar == null || zone.koordinatlar.size() < 3) continue;

            List<LatLng> pts = new ArrayList<>();
            for (List<Double> coord : zone.koordinatlar) {
                // koordinatlar [lng, lat] → LatLng(lat, lng)
                pts.add(new LatLng(coord.get(1), coord.get(0)));
            }

            Polygon poly = googleMap.addPolygon(new PolygonOptions()
                    .addAll(pts)
                    .strokeColor(Color.parseColor("#4CAF50"))   // yeşil kenar
                    .strokeWidth(4)
                    .fillColor(Color.argb(40, 76, 175, 80)));   // şeffaf yeşil dolgu
            poly.setTag(zone.alanAdi);
            newPolygons.add(poly);
        }
        geofencePolygons.put(childId, newPolygons);
    }

    /**
     * Harita FAB'ına basınca:
     *  - 1 çocuk varsa → direkt GeofenceDrawFragment aç
     *  - Birden fazla çocuk varsa → seçim dialogu göster
     *  - Henüz çocuk yüklenmemişse → yükleyip tekrar dene
     */
    private void openGeofenceDrawForChild() {
        List<MapChildDto> children = viewModel.getMapChildren().getValue();

        if (children == null || children.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Çocuk listesi yükleniyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show();
            viewModel.loadMapChildren();
            return;
        }

        if (children.size() == 1) {
            navigateToGeofenceDraw(children.get(0));
            return;
        }

        // Birden fazla çocuk — seçim dialogu
        String[] names = new String[children.size()];
        for (int i = 0; i < children.size(); i++) {
            names[i] = children.get(i).childName;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Hangi çocuk için bölge eklensin?")
                .setItems(names, (dialog, which) ->
                        navigateToGeofenceDraw(children.get(which)))
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void navigateToGeofenceDraw(MapChildDto child) {
        // lat/lng doğrudan MapChildDto'dan al → çocuk neredeyse harita oraya açılır
        double lat = child.lat != null ? child.lat : 0.0;
        double lng = child.lng != null ? child.lng : 0.0;
        GeofenceDrawFragment drawFragment =
                GeofenceDrawFragment.newInstance(child.childId, child.childName, lat, lng);
        ((MainActivity) requireActivity()).loadFragment(drawFragment, true);
    }

    /** Tüm çocukların geofence'lerini yeniden yükler */
    private void refreshAllGeofences() {
        List<MapChildDto> children = viewModel.getMapChildren().getValue();
        if (children == null) return;
        for (MapChildDto c : children) {
            viewModel.loadGeofences(c.childId);
        }
    }

    // ── SSE ───────────────────────────────────────────────────────────────────

    private void startSse() {
        String token = session.getToken();
        if (token == null) return;

        OkHttpClient client = ApiClient.getAuthOkHttpClient(token);
        Request request = new Request.Builder()
                .url(ApiClient.BASE_URL + "parent/children/live")
                .build();

        eventSource = EventSources.createFactory(client)
                .newEventSource(request, new EventSourceListener() {

                    @Override
                    public void onOpen(@NonNull EventSource source,
                                       @NonNull okhttp3.Response response) {
                        runOnUi(() -> setSseStatus("Canlı", Color.GREEN));
                    }

                    @Override
                    public void onEvent(@NonNull EventSource source,
                                        @Nullable String id,
                                        @Nullable String type,
                                        @NonNull String data) {
                        if ("geofence-breach".equals(type)) {
                            // ── Güvenli bölge ihlali ──────────────────────────
                            GeofenceBreachEvent breach =
                                    gson.fromJson(data, GeofenceBreachEvent.class);
                            runOnUi(() -> handleGeofenceBreach(breach));
                        } else {
                            // ── Normal konum güncellemesi ─────────────────────
                            try {
                                MapChildDto child = gson.fromJson(data, MapChildDto.class);
                                runOnUi(() -> {
                                    if (binding != null) addOrUpdateMarker(child);
                                });
                            } catch (Exception ignored) {}
                        }
                    }

                    @Override
                    public void onFailure(@NonNull EventSource source,
                                          @Nullable Throwable t,
                                          @Nullable okhttp3.Response response) {
                        runOnUi(() -> setSseStatus("Bağlantı kesildi", Color.RED));
                    }

                    @Override
                    public void onClosed(@NonNull EventSource source) {
                        runOnUi(() -> setSseStatus("Kapalı", Color.GRAY));
                    }
                });
    }

    // ── Geofence Breach Handling ───────────────────────────────────────────────

    private void handleGeofenceBreach(GeofenceBreachEvent breach) {
        if (breach == null || getContext() == null) return;

        // 1. Yüksek öncelikli bildirim göster
        showBreachNotification(breach);

        // 2. Uygulama ön plandaysa AlertDialog göster
        showBreachDialog(breach);

        // 3. Haritada çocuğun konumuna zoom yap
        if (googleMap != null) {
            LatLng pos = new LatLng(breach.lat, breach.lng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
            // Mevcut marker'ı kırmızıya çevir (uyarı işareti)
            Marker m = markerMap.get(breach.cocukId);
            if (m != null) {
                m.setTitle("⚠️ " + breach.cocukAdi + " — " + breach.alanAdi + " dışında!");
                m.showInfoWindow();
            }
        }
    }

    private void showBreachNotification(GeofenceBreachEvent breach) {
        if (getContext() == null) return;

        NotificationManager nm = (NotificationManager)
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String title   = "⚠️ Güvenli Bölge İhlali!";
        String message = breach.cocukAdi + ", \"" + breach.alanAdi
                + "\" bölgesinin dışına çıktı!";

        Notification notification = new NotificationCompat.Builder(
                requireContext(), CHANNEL_GEOFENCE)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setColor(Color.RED)
                .build();

        // Her ihlal için farklı ID → birden fazla bildirim birbirini silmez
        nm.notify(NOTIF_ID_BASE + notifCounter.incrementAndGet(), notification);
    }

    private void showBreachDialog(GeofenceBreachEvent breach) {
        if (!isAdded() || getContext() == null) return;

        String message = breach.cocukAdi
                + " \"" + breach.alanAdi + "\" bölgesinin dışına çıktı!\n\n"
                + "Zaman: " + breach.zaman;

        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Güvenli Bölge İhlali!")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Haritada Gör", (d, w) -> {
                    if (googleMap != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(breach.lat, breach.lng), 16f));
                    }
                })
                .setNegativeButton("Tamam", null)
                .show();
    }

    // ── Notification Channel ──────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (getContext() == null) return;
        NotificationManager nm = (NotificationManager)
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_GEOFENCE,
                "Güvenli Bölge Uyarıları",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Çocuğun güvenli bölge dışına çıktığında bildirim gönderilir.");
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setSseStatus(String text, int dotColor) {
        if (binding == null) return;
        binding.tvSseStatus.setText(text);
        // circle_indicator shape'in rengini programatik olarak değiştir
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(dotColor);
        binding.viewSseIndicator.setBackground(dot);
    }

    private void runOnUi(Runnable r) {
        if (getActivity() != null) getActivity().runOnUiThread(r);
    }

    @Override
    public void onDestroyView() {
        if (eventSource != null) {
            eventSource.cancel();
            eventSource = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
