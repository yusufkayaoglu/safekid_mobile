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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.safekid.mobile.databinding.FragmentAiAlertsBinding;
import com.safekid.mobile.viewmodel.ParentViewModel;

import java.util.List;

import com.safekid.mobile.network.dto.AlertDto;

public class AiAlertsFragment extends Fragment {

    private FragmentAiAlertsBinding binding;
    private ParentViewModel viewModel;
    private AiAlertAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);

        setupRecyclerView();
        observeViewModel();

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadAlerts());

        viewModel.loadAlerts();
    }

    private void setupRecyclerView() {
        adapter = new AiAlertAdapter();
        adapter.setListener(alertId -> viewModel.acknowledgeAlert(alertId));
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAlerts.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefresh.setRefreshing(loading);
            binding.progress.setVisibility(View.GONE); // SwipeRefresh handles spinner
        });

        viewModel.getAlerts().observe(getViewLifecycleOwner(), alerts -> {
            updateUI(alerts);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void updateUI(List<AlertDto> alerts) {
        adapter.setItems(alerts);
        boolean empty = alerts == null || alerts.isEmpty();
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvAlerts.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
