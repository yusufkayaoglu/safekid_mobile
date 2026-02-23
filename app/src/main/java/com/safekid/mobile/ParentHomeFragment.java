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

import com.safekid.mobile.databinding.FragmentParentHomeBinding;
import com.safekid.mobile.network.dto.ChildDto;
import com.safekid.mobile.session.SessionManager;
import com.safekid.mobile.viewmodel.ParentViewModel;

import java.util.List;

public class ParentHomeFragment extends Fragment {

    private FragmentParentHomeBinding binding;
    private ParentViewModel viewModel;
    private SessionManager session;
    private ChildListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentParentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);
        session = new SessionManager(requireContext());

        setupRecyclerView();
        setupToolbar();
        observeViewModel();
        setupClickListeners();

        viewModel.refreshApiClient();
        viewModel.loadChildren();
    }

    private void setupToolbar() {
        String name = session.getParentName();
        if (name != null) {
            binding.tvWelcome.setText("Merhaba, " + name + "!");
        }
    }

    private void setupRecyclerView() {
        adapter = new ChildListAdapter();
        adapter.setListener(child -> openChildDetail(child));
        binding.rvChildren.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChildren.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading ->
                binding.swipeRefresh.setRefreshing(loading));

        viewModel.getChildren().observe(getViewLifecycleOwner(), children -> {
            updateUI(children);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void updateUI(List<ChildDto> children) {
        adapter.setItems(children);
        boolean isEmpty = children == null || children.isEmpty();
        binding.rvChildren.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (!isEmpty) {
            binding.tvChildCount.setText(children.size() + " çocuk takip ediliyor");
        } else {
            binding.tvChildCount.setText("Henüz çocuk eklenmedi");
        }
    }

    private void setupClickListeners() {
        binding.fabAddChild.setOnClickListener(v -> openAddChild());
        binding.btnAddChildEmpty.setOnClickListener(v -> openAddChild());

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadChildren());
    }

    private void openAddChild() {
        ((MainActivity) requireActivity()).loadFragment(new AddChildFragment(), true);
    }

    private void openChildDetail(ChildDto child) {
        ChildDetailFragment fragment = ChildDetailFragment.newInstance(
                child.cocukUniqueId, child.getFullName(), child.cocukMail);
        ((MainActivity) requireActivity()).loadFragment(fragment, true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
