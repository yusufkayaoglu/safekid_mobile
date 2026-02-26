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

import com.safekid.mobile.databinding.FragmentRegisterBinding;
import com.safekid.mobile.network.dto.RegisterRequest;
import com.safekid.mobile.viewmodel.AuthViewModel;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;
    private String pendingEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
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
            binding.btnRegister.setEnabled(!loading);
        });

        viewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success == null || !success) return;
            Toast.makeText(requireContext(),
                    "Kayıt başarılı! E-postanızı doğrulayın.", Toast.LENGTH_LONG).show();
            // E-posta doğrulama ekranına geç
            VerifyEmailFragment fragment = VerifyEmailFragment.newInstance(pendingEmail);
            ((MainActivity) requireActivity()).loadFragment(fragment, true);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> {
            String adi       = text(binding.etAdi);
            String soyadi    = text(binding.etSoyadi);
            String userCode  = text(binding.etUserCode);
            String mail      = text(binding.etMail);
            String password  = text(binding.etPassword);
            String adres     = text(binding.etAdres);
            String isAdres   = text(binding.etIsAdresi);

            if (adi.isEmpty() || soyadi.isEmpty() || userCode.isEmpty()
                    || mail.isEmpty() || password.isEmpty() || adres.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Zorunlu alanları doldurun", Toast.LENGTH_SHORT).show();
                return;
            }

            pendingEmail = mail;

            RegisterRequest req = new RegisterRequest();
            req.ebeveynAdi      = adi;
            req.ebeveynSoyadi   = soyadi;
            req.ebeveynUserCode = userCode;
            req.ebeveynMailAdres = mail;
            req.ebeveynPassword = password;
            req.ebeveynAdres    = adres;
            req.ebeveynIsAdresi = isAdres;

            viewModel.register(req);
        });

        binding.tvGoLogin.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private String text(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
