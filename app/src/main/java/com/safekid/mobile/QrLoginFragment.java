package com.safekid.mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.safekid.mobile.databinding.FragmentQrLoginBinding;
import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.ParentApi;
import com.safekid.mobile.network.dto.FcmTokenRequest;
import com.safekid.mobile.session.SessionManager;
import com.safekid.mobile.viewmodel.AuthViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrLoginFragment extends Fragment {

    private FragmentQrLoginBinding binding;
    private AuthViewModel viewModel;
    private SessionManager session;

    // QR Tarayıcı launcher
    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String cocukUniqueId = result.getContents().trim();
                    binding.tvStatus.setText("Giriş yapılıyor...");
                    viewModel.qrLogin(cocukUniqueId);
                } else {
                    binding.tvStatus.setText("QR tarama iptal edildi");
                }
            });

    // Kamera izni launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            launchQrScanner();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Kamera izni gerekli", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQrLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        session = new SessionManager(requireContext());

        observeViewModel();
        setupClickListeners();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnScanQr.setEnabled(!loading);
        });

        viewModel.getQrLoginResult().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            viewModel.clearQrLoginResult();

            // Bu cihaz daha önce ebeveyn olarak kullanıldıysa FCM token'ını backend'den sil.
            // Aksi hâlde backend bu cihazı hâlâ ebeveyn cihazı sanır ve
            // geofence ihlali bildirimlerini buraya gönderir.
            if (session.isParent()) {
                String parentJwt = session.getToken();
                if (parentJwt != null) {
                    ApiClient.getAuthInstance(parentJwt)
                            .create(ParentApi.class)
                            .updateFcmToken(new FcmTokenRequest(""))
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(@NonNull Call<Void> call,
                                                       @NonNull Response<Void> r) {
                                    android.util.Log.d("FCM",
                                            "Ebeveyn FCM token temizlendi (cihaz çocuk moduna geçti)");
                                }
                                @Override
                                public void onFailure(@NonNull Call<Void> call,
                                                      @NonNull Throwable t) {
                                    android.util.Log.w("FCM",
                                            "FCM token temizlenemedi: " + t.getMessage());
                                }
                            });
                }
            }

            // Çocuk girişi — CHILD token kaydediliyor
            session.saveChildLogin(res.accessToken, res.expiresAt,
                    res.ebeveynUniqueId, res.ebeveynAdi, res.ebeveynSoyadi);
            Toast.makeText(requireContext(), "Giriş başarılı!", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).navigateToChildActive();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                binding.tvStatus.setText(err);
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnScanQr.setOnClickListener(v -> checkCameraAndScan());

        binding.tvGoParentLogin.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void checkCameraAndScan() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchQrScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchQrScanner() {
        ScanOptions options = new ScanOptions()
                .setPrompt("Ebeveyn uygulamasındaki QR kodu taratın")
                .setOrientationLocked(false)
                .setBeepEnabled(true);
        qrScanLauncher.launch(options);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
