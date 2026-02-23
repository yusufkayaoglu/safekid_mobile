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

import com.safekid.mobile.databinding.FragmentAiChatBinding;
import com.safekid.mobile.viewmodel.ParentViewModel;

public class AiChatFragment extends Fragment {

    private static final String ARG_CHILD_ID   = "child_id";
    private static final String ARG_CHILD_NAME = "child_name";

    private FragmentAiChatBinding binding;
    private ParentViewModel viewModel;
    private AiChatAdapter adapter;
    private String childId;
    private String childName;

    public static AiChatFragment newInstance(String childId, String childName) {
        AiChatFragment f = new AiChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_ID, childId);
        args.putString(ARG_CHILD_NAME, childName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            childId   = getArguments().getString(ARG_CHILD_ID, "");
            childName = getArguments().getString(ARG_CHILD_NAME, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);

        binding.toolbar.setTitle(childName + " – AI Chat");
        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        setupRecyclerView();
        observeViewModel();
        setupClickListeners();

        // Karşılama mesajı
        adapter.addMessage(new ChatMessage(
                "Merhaba! " + childName + " hakkında ne sormak istersiniz?",
                ChatMessage.TYPE_AI));
    }

    private void setupRecyclerView() {
        adapter = new AiChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(lm);
        binding.rvChat.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.layoutTyping.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSend.setEnabled(!loading);
        });

        viewModel.getChatReply().observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            adapter.addMessage(new ChatMessage(res.reply, ChatMessage.TYPE_AI));
            binding.rvChat.scrollToPosition(adapter.getItemCount() - 1);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        binding.btnSend.setOnClickListener(v -> {
            String message = binding.etMessage.getText() != null
                    ? binding.etMessage.getText().toString().trim() : "";
            if (message.isEmpty()) return;

            adapter.addMessage(new ChatMessage(message, ChatMessage.TYPE_USER));
            binding.rvChat.scrollToPosition(adapter.getItemCount() - 1);
            binding.etMessage.setText("");

            viewModel.sendMessage(childId, message);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
