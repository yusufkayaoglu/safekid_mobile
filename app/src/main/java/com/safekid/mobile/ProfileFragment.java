package com.safekid.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.safekid.mobile.databinding.FragmentProfileBinding;
import com.safekid.mobile.session.SessionManager;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());

        binding.tvParentName.setText(session.getFullName());
        binding.tvParentId.setText("ID: " + session.getParentId());
        binding.tvTokenExpiry.setText("Token Bitiş: " + session.getExpiresAt());

        binding.btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Çıkış Yap")
                    .setMessage("Hesabınızdan çıkış yapmak istiyor musunuz?")
                    .setPositiveButton("Çıkış", (d, w) -> {
                        session.clear();
                        ((MainActivity) requireActivity()).navigateToLogin();
                    })
                    .setNegativeButton("Vazgeç", null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
