package com.safekid.mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.safekid.mobile.databinding.FragmentChildActiveBinding;
import com.safekid.mobile.service.LocationService;
import com.safekid.mobile.service.LocationWorker;
import com.safekid.mobile.session.SessionManager;

import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChildActiveFragment extends Fragment {

    private FragmentChildActiveBinding binding;
    private SessionManager session;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshLastSentRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding == null) return;
            long lastSentAt = session.getLastLocationSentAt();
            if (lastSentAt > 0) {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(new Date(lastSentAt));
                binding.tvLastSent.setText("Son gönderim: " + time);
            }
            uiHandler.postDelayed(this, 5_000);
        }
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean fineGranted = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                        if (fineGranted) {
                            startTracking();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Konum izni gerekli", Toast.LENGTH_LONG).show();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChildActiveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());

        binding.btnStop.setOnClickListener(v -> stopAndLogout());

        checkPermissionsAndStart();
        observeWorkerState();
        uiHandler.post(refreshLastSentRunnable);
    }

    // ── İzin & Başlatma ───────────────────────────────────────────────────────

    private void checkPermissionsAndStart() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            perms = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        boolean allGranted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(requireContext(), p)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startTracking();
        } else {
            permissionLauncher.launch(perms);
        }
    }

    /**
     * İki mekanizma birlikte çalışır:
     *  1. ForegroundService (LocationService) — anlık, güvenilir, 30sn interval
     *  2. WorkManager (LocationWorker)         — uygulama sonlandırılsa bile devam eder
     *
     * ⚠️ WorkManager PeriodicWorkRequest minimum = 15 dakika (Android kısıtı).
     *    Bu nedenle LocationWorker OneTimeWorkRequest zinciri kullanır (30sn gecikme).
     */
    private void startTracking() {
        // 1. Mevcut ForegroundService'i başlat (30sn GPS — anlık güncelleme)
        Intent intent = new Intent(requireContext(), LocationService.class);
        intent.putExtra(LocationService.EXTRA_TOKEN, session.getToken());
        ContextCompat.startForegroundService(requireContext(), intent);

        // 2. WorkManager zincirini başlat (uygulama arka planda/öldürülünce devreye girer)
        LocationWorker.startNow(requireContext());

        binding.tvServiceStatus.setText("Konum gönderiliyor...");
    }

    // ── WorkManager Durum Takibi ──────────────────────────────────────────────

    /**
     * WorkManager'ın çalışma durumunu LiveData ile UI'a yansıtır.
     */
    private void observeWorkerState() {
        WorkManager.getInstance(requireContext())
                .getWorkInfosForUniqueWorkLiveData(LocationWorker.WORK_NAME)
                .observe(getViewLifecycleOwner(), workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) return;

                    WorkInfo info = workInfos.get(0);
                    switch (info.getState()) {
                        case RUNNING:
                            binding.tvServiceStatus.setText("Konum alınıyor...");
                            break;
                        case ENQUEUED:
                            binding.tvServiceStatus.setText("Konum gönderiliyor... (30sn)");
                            break;
                        case SUCCEEDED:
                            binding.tvServiceStatus.setText("Konum gönderildi");
                            break;
                        case FAILED:
                            binding.tvServiceStatus.setText("Gönderim başarısız, tekrar deniyor...");
                            break;
                        case CANCELLED:
                            binding.tvServiceStatus.setText("Durduruldu");
                            break;
                        default:
                            break;
                    }
                });
    }

    // ── Durdur / Çıkış ────────────────────────────────────────────────────────

    private void stopAndLogout() {
        // ForegroundService durdur
        requireContext().stopService(new Intent(requireContext(), LocationService.class));

        // WorkManager zincirini iptal et
        LocationWorker.cancel(requireContext());

        session.clear();
        Toast.makeText(requireContext(), "Çıkış yapıldı", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        uiHandler.removeCallbacks(refreshLastSentRunnable);
        super.onDestroyView();
        binding = null;
    }
}
