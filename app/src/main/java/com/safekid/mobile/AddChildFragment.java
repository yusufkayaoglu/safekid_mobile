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

import com.safekid.mobile.databinding.FragmentAddChildBinding;
import com.safekid.mobile.network.dto.ChildDto;
import com.safekid.mobile.viewmodel.ParentViewModel;

public class AddChildFragment extends Fragment {

    private FragmentAddChildBinding binding;
    private ParentViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddChildBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        observeViewModel();
        setupClickListeners();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!loading);
        });

        viewModel.getAddedChild().observe(getViewLifecycleOwner(), child -> {
            if (child == null) return;
            viewModel.clearAddedChild(); // Tekrar tetiklenmeyi önle
            Toast.makeText(requireContext(),
                    child.getFullName() + " eklendi!", Toast.LENGTH_SHORT).show();
            viewModel.loadChildren();
            ChildDetailFragment detail = ChildDetailFragment.newInstance(
                    child.cocukUniqueId, child.getFullName(), child.cocukMail);
            ((MainActivity) requireActivity()).loadFragment(detail, true);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnSave.setOnClickListener(v -> {
            String adi = text(binding.etCocukAdi);
            String soyadi = text(binding.etCocukSoyadi);
            String telefon = text(binding.etCocukTelefon);
            String mail = text(binding.etCocukMail);

            if (adi.isEmpty() || soyadi.isEmpty()) {
                Toast.makeText(requireContext(), "Ad ve soyad zorunlu", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.addChild(adi, soyadi, telefon, mail);
        });
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
