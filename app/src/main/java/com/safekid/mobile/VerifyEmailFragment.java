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

import com.safekid.mobile.databinding.FragmentVerifyEmailBinding;
import com.safekid.mobile.viewmodel.AuthViewModel;

public class VerifyEmailFragment extends Fragment {

    private static final String ARG_EMAIL = "email";

    private FragmentVerifyEmailBinding binding;
    private AuthViewModel viewModel;
    private String email;

    public static VerifyEmailFragment newInstance(String email) {
        VerifyEmailFragment f = new VerifyEmailFragment();
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
        binding = FragmentVerifyEmailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.tvEmailHint.setText("E-posta: " + email + "\n6 haneli kodu girin.");

        observeViewModel();
        setupClickListeners();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnVerify.setEnabled(!loading);
        });

        viewModel.getVerifySuccess().observe(getViewLifecycleOwner(), success -> {
            if (success == null || !success) return;
            Toast.makeText(requireContext(),
                    "E-posta doğrulandı! Giriş yapabilirsiniz.", Toast.LENGTH_LONG).show();
            ((MainActivity) requireActivity()).navigateToLogin();
        });

        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                viewModel.clearOperationSuccess();
                Toast.makeText(requireContext(), "Kod tekrar gönderildi.", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnVerify.setOnClickListener(v -> {
            String code = binding.etCode.getText() != null
                    ? binding.etCode.getText().toString().trim() : "";
            if (code.length() != 6) {
                Toast.makeText(requireContext(), "6 haneli kod girin", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.verifyEmail(email, code);
        });

        binding.tvResend.setOnClickListener(v -> viewModel.resendVerification(email));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
