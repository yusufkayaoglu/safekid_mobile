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

import com.safekid.mobile.databinding.FragmentForgotPasswordBinding;
import com.safekid.mobile.viewmodel.AuthViewModel;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
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
            binding.btnSend.setEnabled(!loading);
        });

        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success == null || !success) return;
            viewModel.clearOperationSuccess();
            String email = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim() : "";
            Toast.makeText(requireContext(),
                    "Kod gönderildi. E-postanızı kontrol edin.", Toast.LENGTH_LONG).show();
            ResetPasswordFragment fragment = ResetPasswordFragment.newInstance(email);
            ((MainActivity) requireActivity()).loadFragment(fragment, true);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnSend.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "E-posta girin", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.forgotPassword(email);
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
