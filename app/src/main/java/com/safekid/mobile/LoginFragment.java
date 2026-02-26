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

import com.google.firebase.messaging.FirebaseMessaging;
import com.safekid.mobile.databinding.FragmentLoginBinding;
import com.safekid.mobile.session.SessionManager;
import com.safekid.mobile.viewmodel.AuthViewModel;
import com.safekid.mobile.viewmodel.ParentViewModel;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;
    private ParentViewModel parentViewModel;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        parentViewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);
        session = new SessionManager(requireContext());

        observeViewModel();
        setupClickListeners();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!loading);
            binding.etEmail.setEnabled(!loading);
            binding.etPassword.setEnabled(!loading);
        });

        viewModel.getLoginResult().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            viewModel.clearLoginResult();
            session.saveLogin(res.accessToken, res.expiresAt,
                    res.ebeveynUniqueId, res.ebeveynAdi, res.ebeveynSoyadi);
            parentViewModel.refreshApiClient();
            Toast.makeText(requireContext(),
                    "Hoşgeldin " + res.ebeveynAdi, Toast.LENGTH_SHORT).show();
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(fcmToken -> {
                        session.saveFcmToken(fcmToken);
                        parentViewModel.updateFcmToken(fcmToken);
                    });
            ((MainActivity) requireActivity()).navigateToParentHome();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim() : "";
            String password = binding.etPassword.getText() != null
                    ? binding.etPassword.getText().toString() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Alanlar boş olamaz", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.login(email, password);
        });

        binding.tvGoRegister.setOnClickListener(v ->
                ((MainActivity) requireActivity()).loadFragment(new RegisterFragment(), true));

        binding.tvForgotPassword.setOnClickListener(v ->
                ((MainActivity) requireActivity()).loadFragment(new ForgotPasswordFragment(), true));

        binding.tvChildMode.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).showBottomNav(false);
            ((MainActivity) requireActivity()).loadFragment(new QrLoginFragment(), true);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
