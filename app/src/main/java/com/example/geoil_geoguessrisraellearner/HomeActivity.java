package com.example.geoil_geoguessrisraellearner;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        viewPager.setAdapter(new GamePagerAdapter(this));
        viewPager.setOffscreenPageLimit(5);
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_account) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.nav_learn) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(2, true);
                return true;
            } else if (itemId == R.id.nav_challenges) {
                viewPager.setCurrentItem(3, true);
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                viewPager.setCurrentItem(4, true);
                return true;
            }

            return false;
        });

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        bottomNav.getMenu().getItem(position).setChecked(true);
                    }
                }
        );

        // Start on Home (middle tab)
        viewPager.setCurrentItem(2, false);
    }
}
