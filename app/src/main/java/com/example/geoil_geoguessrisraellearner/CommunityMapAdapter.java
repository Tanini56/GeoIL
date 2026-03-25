package com.example.geoil_geoguessrisraellearner;

import android.content.Context;
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
        // Ensure this matches your XML file name for the list item
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

        // 2. If for some reason iconName is null, fallback to the category-based name
        if (iconResourceName == null && currentMap.getCategory() != null) {
            iconResourceName = currentMap.getCategory().toLowerCase() + "_icon";
        }

        if (iconResourceName != null) {
            // 3. Look up the resource ID dynamically
            int resID = context.getResources().getIdentifier(iconResourceName, "drawable", context.getPackageName());

            if (resID != 0) {
                holder.imgCategoryIcon.setImageResource(resID);
            } else {
                // Fallback if the file is missing from your drawables folder
                holder.imgCategoryIcon.setImageResource(R.drawable.city_icon);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMapClick(currentMap);
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