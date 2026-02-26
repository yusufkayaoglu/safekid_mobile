package com.safekid.mobile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.safekid.mobile.databinding.FragmentGeofenceDrawBinding;
import com.safekid.mobile.network.dto.GeofenceDto;
import com.safekid.mobile.viewmodel.ParentViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Ebeveynin Google Maps üzerinde polygon çizerek güvenli bölge oluşturduğu ekran.
 *
 * Kullanım:
 *   ((MainActivity) requireActivity()).loadFragment(
 *       GeofenceDrawFragment.newInstance(childId, childName), true);
 *
 * Koordinat sırası: GeoJSON standardı → [longitude, latitude]
 * Google Maps LatLng kullanırken: önce .longitude, sonra .latitude alınır.
 */
public class GeofenceDrawFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_CHILD_ID   = "child_id";
    private static final String ARG_CHILD_NAME = "child_name";
    private static final String ARG_CHILD_LAT  = "child_lat";
    private static final String ARG_CHILD_LNG  = "child_lng";

    private static final int FILL_ALPHA   = 50;
    private static final int STROKE_WIDTH = 5;

    private FragmentGeofenceDrawBinding binding;
    private ParentViewModel viewModel;
    private com.safekid.mobile.session.SessionManager session;
    private GoogleMap googleMap;

    private String childId;
    private String childName;
    private double childLat;
    private double childLng;

    /** Çocuğun konumunu gösteren sabit marker (polygon noktalarından farklı) */
    private Marker childLocationMarker;

    // Çizilen noktalar
    private final List<LatLng> points = new ArrayList<>();
    // Haritadaki görseller
    private Polygon activePolygon;
    private Polyline closingLine;          // son noktadan ilk noktaya kapanma çizgisi
    private final List<Marker> pointMarkers = new ArrayList<>();
    // Mevcut (kayıtlı) bölgeler
    private final Map<Long, Polygon> savedPolygons = new HashMap<>();

    // "Temizle" sonrası LiveData yeniden tetiklenirse polygon'ları yeniden çizme
    private boolean savedZonesCleared = false;

    private boolean drawingMode = false;

    // ── Factory ──────────────────────────────────────────────────────────────

    /**
     * @param childLat  Çocuğun mevcut enlemi  (0.0 → konum bilinmiyor)
     * @param childLng  Çocuğun mevcut boylamı (0.0 → konum bilinmiyor)
     */
    public static GeofenceDrawFragment newInstance(String childId, String childName,
                                                   double childLat, double childLng) {
        GeofenceDrawFragment f = new GeofenceDrawFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_ID,   childId);
        args.putString(ARG_CHILD_NAME, childName);
        args.putDouble(ARG_CHILD_LAT,  childLat);
        args.putDouble(ARG_CHILD_LNG,  childLng);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGeofenceDrawBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            childId   = getArguments().getString(ARG_CHILD_ID, "");
            childName = getArguments().getString(ARG_CHILD_NAME, "");
            childLat  = getArguments().getDouble(ARG_CHILD_LAT, 0.0);
            childLng  = getArguments().getDouble(ARG_CHILD_LNG, 0.0);
        }

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);
        session = new com.safekid.mobile.session.SessionManager(requireContext());

        // Premium gate — geofence sadece Premium kullanıcılara açık
        if (!session.isPremium()) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Premium Özellik")
                    .setMessage("Güvenli Bölge oluşturma yalnızca Premium üyelere açıktır.")
                    .setPositiveButton("Premium'a Geç", (d, w) -> {
                        requireActivity().getSupportFragmentManager().popBackStack();
                        ((MainActivity) requireActivity()).loadFragment(new PremiumFragment(), true);
                    })
                    .setNegativeButton("İptal", (d, w) ->
                            requireActivity().getSupportFragmentManager().popBackStack())
                    .setCancelable(false)
                    .show();
            return;
        }

        binding.toolbar.setTitle(childName + " — Güvenli Bölge");
        binding.toolbar.setNavigationOnClickListener(v -> handleBack());

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBack();
                    }
                });

        setupButtons();
        observeViewModel();

        // Haritayı başlat
        SupportMapFragment mapFrag = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.drawMapContainer, mapFrag)
                .commit();
        mapFrag.getMapAsync(this);
    }

    // ── Button Logic ──────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnToggleDraw.setOnClickListener(v -> toggleDrawMode());

        binding.btnUndo.setOnClickListener(v -> undoLastPoint());

        binding.btnClear.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Temizle")
                    .setMessage("Tüm noktalar silinsin mi?")
                    .setPositiveButton("Evet", (d, w) -> clearAll())
                    .setNegativeButton("Vazgeç", null)
                    .show();
        });

        binding.btnDeleteZone.setOnClickListener(v -> showDeleteZoneDialog());

        binding.btnSave.setOnClickListener(v -> saveGeofence());
    }

    private void toggleDrawMode() {
        drawingMode = !drawingMode;
        if (drawingMode) {
            binding.btnToggleDraw.setText("Çizimi Durdur");
            binding.btnToggleDraw.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
            binding.tvHint.setVisibility(View.VISIBLE);
            binding.tvHint.setText("Haritaya dokunarak köşe noktaları ekleyin");
        } else {
            binding.btnToggleDraw.setText("Polygon Çiz");
            binding.btnToggleDraw.setIconResource(android.R.drawable.ic_menu_edit);
            binding.tvHint.setVisibility(View.GONE);
        }
    }

    private void undoLastPoint() {
        if (points.isEmpty()) return;
        points.remove(points.size() - 1);
        // Son marker'ı sil
        if (!pointMarkers.isEmpty()) {
            pointMarkers.get(pointMarkers.size() - 1).remove();
            pointMarkers.remove(pointMarkers.size() - 1);
        }
        refreshDrawing();
    }

    private void clearAll() {

        points.clear();

        for (Marker m : pointMarkers) m.remove();
        pointMarkers.clear();

        if (activePolygon != null) {
            activePolygon.remove();
            activePolygon = null;
        }

        if (closingLine != null) {
            closingLine.remove();
            closingLine = null;
        }

        // Flag'i EN ÖNCE set et — aynı anda tetiklenebilecek drawSavedZones'u blokla
        savedZonesCleared = true;
        // ViewModel'deki LiveData'yı da sıfırla — sticky re-delivery'de null gelir, çizilmez
        viewModel.clearGeofences();

        // Kayıtlı (mavi) polygon'ları haritadan kaldır
        for (Polygon p : savedPolygons.values()) {
            p.remove();
        }
        savedPolygons.clear();

        updatePointCount();
    }

    private void saveGeofence() {
        if (points.size() < 3) {
            Toast.makeText(requireContext(),
                    "En az 3 köşe noktası gerekli", Toast.LENGTH_SHORT).show();
            return;
        }

        String alanAdi = binding.etZoneName.getText() != null
                ? binding.etZoneName.getText().toString().trim()
                : "";
        if (alanAdi.isEmpty()) {
            binding.tilZoneName.setError("Bölge adı girin");
            return;
        }
        binding.tilZoneName.setError(null);

        // Google Maps LatLng → [longitude, latitude] (GeoJSON sırası)
        List<List<Double>> koordinatlar = new ArrayList<>();
        for (LatLng p : points) {
            koordinatlar.add(Arrays.asList(p.longitude, p.latitude));
        }
        // GeoJSON standartı: ilk nokta = son nokta (kapalı halka)
        koordinatlar.add(Arrays.asList(points.get(0).longitude, points.get(0).latitude));

        viewModel.createGeofence(childId, alanAdi, koordinatlar);
    }

    // ── ViewModel Observers ───────────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.getSavedGeofence().observe(getViewLifecycleOwner(), dto -> {
            if (dto == null) return;
            viewModel.clearSavedGeofence();
            Toast.makeText(requireContext(),
                    "\"" + dto.alanAdi + "\" kaydedildi!", Toast.LENGTH_LONG).show();
            ((MainActivity) requireActivity()).navigateToParentHome();
        });

        viewModel.getGeofences().observe(getViewLifecycleOwner(), this::drawSavedZones);

        viewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (!Boolean.TRUE.equals(success)) return;
            viewModel.clearDeleteSuccess();
            Toast.makeText(requireContext(), "Bölge silindi", Toast.LENGTH_SHORT).show();
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnSave.setEnabled(!Boolean.TRUE.equals(loading));
            binding.progressSave.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null)
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });
    }

    // ── Map ───────────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Kullanıcı haritaya dokunduğunda nokta ekle
        googleMap.setOnMapClickListener(latLng -> {
            if (!drawingMode) return;
            addPoint(latLng);
        });

        // Çocuğun mevcut konumunu haritada göster ve kamerayı oraya götür
        if (childLat != 0.0 && childLng != 0.0) {
            LatLng childPos = new LatLng(childLat, childLng);

            // Mavi (Azure) marker → çocuğun bulunduğu yer
            // Kırmızı/Yeşil polygon noktalarından kolayca ayırt edilir
            childLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(childPos)
                    .title(childName + " — mevcut konum")
                    .snippet("Güvenli bölgeyi bu konumun etrafına çizin")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE))
                    .zIndex(1.0f));

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(childPos, 15f));

            // Açıklama hint'ini göster
            if (binding != null) {
                binding.tvChildLocationHint.setVisibility(View.VISIBLE);
            }
        } else {
            // Konum bilinmiyorsa Türkiye genel görünümü
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(39.9334, 32.8597), 6f));
        }

        // Kayıtlı geofence'leri yükle
        viewModel.loadGeofences(childId);
    }

    private void addPoint(LatLng latLng) {
        points.add(latLng);

        // Nokta numarası marker'ı
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Nokta " + points.size())
                .icon(BitmapDescriptorFactory.defaultMarker(
                        points.size() == 1
                                ? BitmapDescriptorFactory.HUE_GREEN
                                : BitmapDescriptorFactory.HUE_RED))
                .anchor(0.5f, 0.5f));
        pointMarkers.add(marker);

        refreshDrawing();
    }

    /** Polygon ve kapanma çizgisini noktalar listesine göre yeniden çizer */
    private void refreshDrawing() {
        // Önceki polygon/çizgiyi kaldır
        if (activePolygon != null) { activePolygon.remove(); activePolygon = null; }
        if (closingLine != null)   { closingLine.remove(); closingLine = null; }

        updatePointCount();

        if (points.size() < 2) return;

        if (points.size() >= 3) {
            // 3+ nokta → kapalı polygon çiz
            activePolygon = googleMap.addPolygon(new PolygonOptions()
                    .addAll(points)
                    .strokeColor(Color.RED)
                    .strokeWidth(STROKE_WIDTH)
                    .fillColor(Color.argb(FILL_ALPHA, 255, 50, 50)));
        } else {
            // 2 nokta → sadece kapanma çizgisi yok, düz çizgi göster
            closingLine = googleMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(Color.RED)
                    .width(STROKE_WIDTH));
        }
    }

    private void updatePointCount() {
        int n = points.size();
        binding.tvPointCount.setText(n + " nokta" + (n < 3 ? " (min. 3)" : " ✓"));
        binding.btnSave.setEnabled(n >= 3);
    }

    /** Sunucudan gelen kayıtlı bölgeleri mavi renkte haritada göster */

    private void drawSavedZones(List<GeofenceDto> zones) {

        if (googleMap == null) return;

        // "Temizle" sonrası: flag veya null veri → haritayı temizle, çizme
        if (savedZonesCleared || zones == null) {
            for (Polygon p : savedPolygons.values()) p.remove();
            savedPolygons.clear();
            return;
        }

        // 🔥 1) SERVERDA OLMAYAN POLYGONLARI SİL
        List<Long> incomingIds = new ArrayList<>();

        if (zones != null) {
            for (GeofenceDto z : zones) {
                incomingIds.add(z.id);
            }
        }

        Iterator<Map.Entry<Long, Polygon>> it = savedPolygons.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Long, Polygon> entry = it.next();

            if (!incomingIds.contains(entry.getKey())) {
                entry.getValue().remove();
                it.remove();
            }
        }

        if (zones == null) return;

        // 🔵 2) POLYGONLARI GÜNCELLE (🔥 ESKİYİ REMOVE ET, YENİYİ ÇİZ)
        for (GeofenceDto zone : zones) {

            if (zone.koordinatlar == null || zone.koordinatlar.size() < 3)
                continue;

            // 🔥 EĞER VARSA ÖNCE ESKİYİ SİL
            if (savedPolygons.containsKey(zone.id)) {
                savedPolygons.get(zone.id).remove();
                savedPolygons.remove(zone.id);
            }

            List<LatLng> pts = new ArrayList<>();

            for (List<Double> coord : zone.koordinatlar) {
                pts.add(new LatLng(coord.get(1), coord.get(0)));
            }

            Polygon poly = googleMap.addPolygon(
                    new PolygonOptions()
                            .addAll(pts)
                            .strokeColor(Color.BLUE)
                            .strokeWidth(3)
                            .fillColor(Color.argb(30, 0, 80, 255))
            );

            poly.setTag(zone.alanAdi);

            savedPolygons.put(zone.id, poly);
        }
    }
    /**
     * Kayıtlı bölgeler arasından seçim yaparak silmeyi sağlar.
     * Seçim sonrası onay dialogu → backend'den siler, haritayı günceller.
     */
    private void showDeleteZoneDialog() {
        List<GeofenceDto> zones = viewModel.getGeofences().getValue();

        if (zones == null || zones.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Silinecek kayıtlı bölge yok", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[zones.size()];
        for (int i = 0; i < zones.size(); i++) {
            names[i] = zones.get(i).alanAdi;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Hangi bölge silinsin?")
                .setItems(names, (dialog, which) -> {
                    GeofenceDto selected = zones.get(which);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Bölgeyi Sil")
                            .setMessage("\"" + selected.alanAdi + "\" kalıcı olarak silinsin mi?")
                            .setPositiveButton("Sil", (d, w) -> {
                                // Silme sonrası drawSavedZones haritayı güncelleyebilsin
                                savedZonesCleared = false;
                                viewModel.deleteGeofence(selected.id, childId);
                            })
                            .setNegativeButton("Vazgeç", null)
                            .show();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    /**
     * Toolbar geri oku veya sistem geri tuşuna basıldığında çağrılır.
     * Çizilmiş kaydedilmemiş nokta varsa onay dialogu gösterir.
     */
    private void handleBack() {
        if (!points.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Çıkmak istiyor musunuz?")
                    .setMessage("Çizilen " + points.size() + " nokta kaydedilmedi. Çıkarsanız kaybolur.")
                    .setPositiveButton("Çık", (d, w) ->
                            ((MainActivity) requireActivity()).navigateToParentHome())
                    .setNegativeButton("İptal", null)
                    .show();
        } else {
            ((MainActivity) requireActivity()).navigateToParentHome();
        }
    }

    @Override
    public void onDestroyView() {
        if (childLocationMarker != null) {
            childLocationMarker.remove();
            childLocationMarker = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
