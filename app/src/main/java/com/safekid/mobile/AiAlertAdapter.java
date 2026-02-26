package com.safekid.mobile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safekid.mobile.databinding.ItemAiAlertBinding;
import com.safekid.mobile.network.dto.AlertDto;

import java.util.ArrayList;
import java.util.List;

public class AiAlertAdapter extends RecyclerView.Adapter<AiAlertAdapter.VH> {

    public interface OnAcknowledgeListener {
        void onAcknowledge(long alertId);
    }

    private final List<AlertDto> items = new ArrayList<>();
    private OnAcknowledgeListener listener;

    public void setListener(OnAcknowledgeListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AlertDto> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAiAlertBinding b = ItemAiAlertBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        final ItemAiAlertBinding b;

        VH(ItemAiAlertBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(AlertDto alert) {
            b.tvAlertType.setText(formatType(alert.analysisType));
            b.tvChildName.setText(alert.cocukAdi != null ? alert.cocukAdi : "");
            b.tvSummary.setText(parseSummary(alert.resultJson));
            b.tvCreatedAt.setText(alert.createdAt != null ? alert.createdAt : "");

            b.btnAcknowledge.setOnClickListener(v -> {
                if (listener != null) listener.onAcknowledge(alert.alertId);
            });
        }

        private String formatType(String type) {
            if (type == null) return "Uyarı";
            switch (type) {
                case "ANOMALY_CHECK":      return "Anomali Tespiti";
                case "ROUTE_PREDICTION":   return "Rota Tahmini";
                case "DAILY_SUMMARY":      return "Günlük Özet";
                default:                   return type;
            }
        }

        private String parseSummary(String json) {
            if (json == null || json.isEmpty()) return "";
            // JSON'dan summary alanını basit regex ile çek
            try {
                int idx = json.indexOf("\"summary\":");
                if (idx >= 0) {
                    int start = json.indexOf('"', idx + 10) + 1;
                    int end   = json.indexOf('"', start);
                    if (start > 0 && end > start) return json.substring(start, end);
                }
            } catch (Exception ignored) { }
            return json.length() > 120 ? json.substring(0, 120) + "…" : json;
        }
    }
}
