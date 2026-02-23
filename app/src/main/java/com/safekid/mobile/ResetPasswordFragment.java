package com.safekid.mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.safekid.mobile.databinding.FragmentResetPasswordBinding;
import com.safekid.mobile.viewmodel.AuthViewModel;

public class ResetPasswordFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

    private FragmentResetPasswordBinding binding;
    private AuthViewModel viewModel;
    private String email;

    public static ResetPasswordFragment newInstance(String email) {
        ResetPasswordFragment f = new ResetPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            email = getArguments().getString(ARG_EMAIL, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        observeViewModel();
        setupClickListeners();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnReset.setEnabled(!loading);
        });

        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success == null || !success) return;
            viewModel.clearOperationSuccess();
            Toast.makeText(requireContext(),
                    "Şifre güncellendi! Giriş yapabilirsiniz.", Toast.LENGTH_LONG).show();
            ((MainActivity) requireActivity()).navigateToLogin();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnReset.setOnClickListener(v -> {
            String kod = binding.etCode.getText() != null
                    ? binding.etCode.getText().toString().trim() : "";
            String yeniSifre = binding.etNewPassword.getText() != null
                    ? binding.etNewPassword.getText().toString() : "";
            String yeniSifreTekrar = binding.etConfirmPassword.getText() != null
                    ? binding.etConfirmPassword.getText().toString() : "";

            if (kod.length() != 6) {
                Toast.makeText(requireContext(), "6 haneli kodu girin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (yeniSifre.length() < 6) {
                Toast.makeText(requireContext(), "Şifre en az 6 karakter olmalı", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!yeniSifre.equals(yeniSifreTekrar)) {
                Toast.makeText(requireContext(), "Şifreler eşleşmiyor", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.resetPassword(email, kod, yeniSifre);
        });

        binding.tvGoBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
