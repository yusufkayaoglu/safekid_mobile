package com.safekid.mobile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safekid.mobile.databinding.ItemChatAiBinding;
import com.safekid.mobile.databinding.ItemChatUserBinding;

import java.util.ArrayList;
import java.util.List;

public class AiChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ChatMessage.TYPE_USER) {
            return new UserVH(ItemChatUserBinding.inflate(inflater, parent, false));
        } else {
            return new AiVH(ItemChatAiBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String text = messages.get(position).text;
        if (holder instanceof UserVH) ((UserVH) holder).b.tvMessage.setText(text);
        else ((AiVH) holder).b.tvMessage.setText(text);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class UserVH extends RecyclerView.ViewHolder {
        final ItemChatUserBinding b;
        UserVH(ItemChatUserBinding b) { super(b.getRoot()); this.b = b; }
    }

    static class AiVH extends RecyclerView.ViewHolder {
        final ItemChatAiBinding b;
        AiVH(ItemChatAiBinding b) { super(b.getRoot()); this.b = b; }
    }
}
