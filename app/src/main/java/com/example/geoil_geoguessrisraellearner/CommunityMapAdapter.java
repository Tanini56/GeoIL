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

        // 1. Display the automated name (e.g., "Park HaYarkon (5 Rounds)")
        holder.tvMapName.setText(currentMap.getMapName());

        // 2. Set the Author Name
        holder.tvAuthor.setText("By: " + currentMap.getAuthor());

        // 3. Dynamic Icon Logic
        // Takes category from DB (e.g., "beach") and looks for "beach_icon" in drawables
        String category = currentMap.getCategory();
        if (category != null) {
            String iconFileName = category.toLowerCase() + "_icon";

            // Look up the resource ID by string name
            int resID = context.getResources().getIdentifier(iconFileName, "drawable", context.getPackageName());

            if (resID != 0) {
                holder.imgCategoryIcon.setImageResource(resID);
            } else {
                // Fallback icon if the specific category icon is missing
                holder.imgCategoryIcon.setImageResource(android.R.drawable.ic_menu_mapmode);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onMapClick(currentMap));
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