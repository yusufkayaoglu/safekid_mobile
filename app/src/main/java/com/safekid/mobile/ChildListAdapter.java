package com.safekid.mobile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safekid.mobile.databinding.ItemChildBinding;
import com.safekid.mobile.network.dto.ChildDto;

import java.util.ArrayList;
import java.util.List;

public class ChildListAdapter extends RecyclerView.Adapter<ChildListAdapter.VH> {

    public interface OnChildClickListener {
        void onDetailClick(ChildDto child);
    }

    private final List<ChildDto> items = new ArrayList<>();
    private OnChildClickListener listener;

    public void setListener(OnChildClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ChildDto> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChildBinding b = ItemChildBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemChildBinding b;

        VH(ItemChildBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(ChildDto child) {
            b.tvChildName.setText(child.getFullName());
            b.tvChildLastSeen.setText(child.cocukMail != null ? child.cocukMail : "-");

            b.btnDetail.setOnClickListener(v -> {
                if (listener != null) listener.onDetailClick(child);
            });
        }
    }
}
