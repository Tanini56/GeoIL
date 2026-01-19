package com.example.geoil_geoguessrisraellearner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class GamePagerAdapter extends FragmentStateAdapter {

    public GamePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new AccountFragment();
            case 1: return new LearnFragment();
            case 2: return new HomeFragment();
            case 3: return new ChallengesFragment();
            case 4: return new LeaderboardFragment();
            default: return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
