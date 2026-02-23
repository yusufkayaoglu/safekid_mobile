package com.safekid.mobile;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.safekid.mobile.databinding.FragmentChildDetailBinding;
import com.safekid.mobile.network.dto.LocationDto;
import com.safekid.mobile.viewmodel.ParentViewModel;

public class ChildDetailFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_CHILD_ID   = "child_id";
    private static final String ARG_CHILD_NAME = "child_name";
    private static final String ARG_CHILD_MAIL = "child_mail";

    private FragmentChildDetailBinding binding;
    private ParentViewModel viewModel;
    private String childId;
    private String childName;
    private String childMail;
    private GoogleMap miniMap;

    public static ChildDetailFragment newInstance(String id, String name, String mail) {
        ChildDetailFragment f = new ChildDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_ID, id);
        args.putString(ARG_CHILD_NAME, name);
        args.putString(ARG_CHILD_MAIL, mail);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            childId   = getArguments().getString(ARG_CHILD_ID, "");
            childName = getArguments().getString(ARG_CHILD_NAME, "");
            childMail = getArguments().getString(ARG_CHILD_MAIL, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChildDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);

        // Toolbar
        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Bilgi
        binding.tvChildName.setText(childName);
        binding.tvChildMail.setText(childMail != null ? childMail : "-");

        // QR Kod oluştur
        generateQrCode(childId);

        // Mini harita
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.miniMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        observeViewModel();
        setupClickListeners();

        viewModel.clearLastLocation();
        viewModel.loadLastLocation(childId);
    }

    private void observeViewModel() {
        viewModel.getLastLocation().observe(getViewLifecycleOwner(), location -> {
            if (location != null) {
                binding.tvLastSeen.setText("Son konum: " + location.recordedAt);
                if (miniMap != null) showLocationOnMap(location);
            } else {
                binding.tvLastSeen.setText("Henüz konum verisi yok");
            }
        });

        viewModel.getAnomalyCheck().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.cardAnalysis.setVisibility(View.VISIBLE);
            binding.progressAnalysis.setVisibility(View.GONE);
            StringBuilder sb = new StringBuilder();
            sb.append(result.summary).append("\n");
            if (result.anomalies != null) {
                for (String a : result.anomalies) sb.append("• ").append(a).append("\n");
            }
            binding.tvAnalysisResult.setText(sb.toString().trim());
        });

        viewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                Toast.makeText(requireContext(), "Çocuk silindi", Toast.LENGTH_SHORT).show();
                viewModel.loadChildren();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                binding.cardAnalysis.setVisibility(View.VISIBLE);
                binding.progressAnalysis.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnAiChat.setOnClickListener(v -> {
            AiChatFragment chatFragment = AiChatFragment.newInstance(childId, childName);
            ((MainActivity) requireActivity()).loadFragment(chatFragment, true);
        });

        binding.btnAiAnalysis.setOnClickListener(v -> {
            binding.cardAnalysis.setVisibility(View.VISIBLE);
            binding.progressAnalysis.setVisibility(View.VISIBLE);
            binding.tvAnalysisResult.setText("");
            viewModel.runAnomalyCheck(childId);
        });

        binding.btnGeofence.setOnClickListener(v -> {
            // Son bilinen konumu al — varsa çizim ekranı oraya açılır
            LocationDto lastLoc = viewModel.getLastLocation().getValue();
            double lat = (lastLoc != null) ? lastLoc.lat : 0.0;
            double lng = (lastLoc != null) ? lastLoc.lng : 0.0;
            GeofenceDrawFragment drawFragment =
                    GeofenceDrawFragment.newInstance(childId, childName, lat, lng);
            ((MainActivity) requireActivity()).loadFragment(drawFragment, true);
        });

        binding.btnDeleteChild.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Çocuğu Sil")
                    .setMessage(childName + " silinsin mi?")
                    .setPositiveButton("Sil", (d, w) -> viewModel.deleteChild(childId))
                    .setNegativeButton("Vazgeç", null)
                    .show();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        miniMap = map;
        miniMap.getUiSettings().setAllGesturesEnabled(false);
        // Son konum zaten geldiyse göster
        LocationDto loc = viewModel.getLastLocation().getValue();
        if (loc != null) showLocationOnMap(loc);
    }

    private void showLocationOnMap(LocationDto loc) {
        LatLng pos = new LatLng(loc.lat, loc.lng);
        miniMap.clear();
        miniMap.addMarker(new MarkerOptions().position(pos).title(childName));
        miniMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
    }

    private void generateQrCode(String content) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 512, 512);
            binding.ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            binding.ivQrCode.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
