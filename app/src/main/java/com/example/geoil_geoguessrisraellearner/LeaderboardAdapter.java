package com.example.geoil_geoguessrisraellearner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<UserScore> userList;

    public LeaderboardAdapter(List<UserScore> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This inflates the individual row layout we created earlier
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserScore user = userList.get(position);

        // Setting the rank (1., 2., 3...)
        holder.rankText.setText((position + 1) + ".");

        // Setting the username and score from the Firestore data
        holder.usernameText.setText(user.username);
        holder.scoreText.setText(String.valueOf(user.score));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, usernameText, scoreText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rank_text);
            usernameText = itemView.findViewById(R.id.username_text);
            scoreText = itemView.findViewById(R.id.score_text);
        }
    }
}