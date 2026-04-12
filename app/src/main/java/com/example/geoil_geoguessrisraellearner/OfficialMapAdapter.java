package com.example.geoil_geoguessrisraellearner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OfficialMapAdapter extends RecyclerView.Adapter<OfficialMapAdapter.ViewHolder> {

    private List<OfficialMap> maps;
    private OnMapClickListener listener;

    public interface OnMapClickListener {
        void onMapClick(OfficialMap map);
    }

    public OfficialMapAdapter(List<OfficialMap> maps, OnMapClickListener listener) {
        this.maps = maps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // CHANGE: Use the new official layout file
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_official_map, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfficialMap map = maps.get(position);
        holder.tvMapName.setText(map.getMapName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMapClick(map);
            }
        });
    }

    @Override
    public int getItemCount() {
        return maps != null ? maps.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMapName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // CHANGE: Match the new ID from item_official_map.xml
            tvMapName = itemView.findViewById(R.id.tv_official_map_name);
        }
    }
}