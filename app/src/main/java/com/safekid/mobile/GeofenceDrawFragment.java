package com.safekid.mobile;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import java.util.List;

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
    private final List<Polygon> savedPolygons = new ArrayList<>();

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

        binding.toolbar.setTitle(childName + " — Güvenli Bölge");
        binding.toolbar.setNavigationOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToParentHome());

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
        // Sadece polygon noktalarını kaldır — çocuk location marker'ına dokunma
        for (Marker m : pointMarkers) m.remove();
        pointMarkers.clear();
        if (activePolygon != null) { activePolygon.remove(); activePolygon = null; }
        if (closingLine != null)   { closingLine.remove(); closingLine = null; }
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
        if (googleMap == null || zones == null) return;
        for (Polygon p : savedPolygons) p.remove();
        savedPolygons.clear();

        for (GeofenceDto zone : zones) {
            if (zone.koordinatlar == null || zone.koordinatlar.size() < 3) continue;
            List<LatLng> pts = new ArrayList<>();
            for (List<Double> coord : zone.koordinatlar) {
                // koordinatlar [lng, lat] → LatLng(lat, lng)
                pts.add(new LatLng(coord.get(1), coord.get(0)));
            }
            Polygon poly = googleMap.addPolygon(new PolygonOptions()
                    .addAll(pts)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(3)
                    .fillColor(Color.argb(30, 0, 80, 255)));
            poly.setTag(zone.alanAdi);
            savedPolygons.add(poly);
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
