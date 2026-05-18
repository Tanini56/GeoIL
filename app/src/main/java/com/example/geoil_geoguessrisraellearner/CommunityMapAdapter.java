package com.example.geoil_geoguessrisraellearner;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommunityMapAdapter extends RecyclerView.Adapter<CommunityMapAdapter.MapViewHolder> {

    private Context context;
    private List<CommunityMap> mapList;
    private OnMapClickListener listener;

    // Kept the interface here in case other parts of your app look for it during compilation
    public interface OnMapClickListener {
        void onMapClick(CommunityMap map);
    }

    public CommunityMapAdapter(Context context, List<CommunityMap> mapList, OnMapClickListener listener) {
        this.context = context;
        this.mapList = mapList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community_map, parent, false);
        return new MapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MapViewHolder holder, int position) {
        CommunityMap currentMap = mapList.get(position);

        holder.tvMapName.setText(currentMap.getMapName());
        holder.tvAuthor.setText("By: " + (currentMap.getAuthor() != null ? currentMap.getAuthor() : "Anonymous"));

        // 1. Get the icon name stored in Firestore (e.g., "desert_icon")
        String iconResourceName = currentMap.getIconName();

        // 2. Fallback to category name if specific icon metadata is missing
        if (iconResourceName == null && currentMap.getCategory() != null) {
            iconResourceName = currentMap.getCategory().toLowerCase() + "_icon";
        }

        if (iconResourceName != null) {
            // 3. Look up the drawable resource ID dynamically
            int resID = context.getResources().getIdentifier(iconResourceName, "drawable", context.getPackageName());

            if (resID != 0) {
                holder.imgCategoryIcon.setImageResource(resID);
            } else {
                // Default fallback image if an asset goes missing
                holder.imgCategoryIcon.setImageResource(R.drawable.city_icon);
            }
        }

        // DIRECT NAVIGATION ROUTING: Completely skips hybrid interface blocks
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NewCommunityGameActivity.class);

            // Passing the Map Name as the ID identifier to pull the specific document from Firestore
            intent.putExtra("SELECTED_MAP_ID", currentMap.getMapId());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return (mapList != null) ? mapList.size() : 0;
    }

    public static class MapViewHolder extends RecyclerView.ViewHolder {
        TextView tvMapName, tvAuthor;
        ImageView imgCategoryIcon;

        public MapViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMapName = itemView.findViewById(R.id.tv_item_map_name);
            tvAuthor = itemView.findViewById(R.id.tv_item_author);
            imgCategoryIcon = itemView.findViewById(R.id.img_item_icon);
        }
    }
}