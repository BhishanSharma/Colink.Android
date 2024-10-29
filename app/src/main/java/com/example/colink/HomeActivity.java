package com.example.colink;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.colink.Loading.LoadingFragment;
import com.example.colink.Tabs.HomeFragment;
import com.example.colink.Tabs.SettingsFragment;
import com.example.colink.Tabs.MusicPlayerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;

public class HomeActivity extends AppCompatActivity {

    private static final int HOME_FRAGMENT_ID = R.id.bottom_home;
    private static final int MUSIC_PLAYER_FRAGMENT_ID = R.id.bottom_music_player;
    private static final int SETTINGS_FRAGMENT_ID = R.id.bottom_settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_home);

        // Initialize views
        FrameLayout frameLayout = findViewById(R.id.FrameLayout);
        int frameLayoutId = frameLayout.getId();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Handle bottom navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == HOME_FRAGMENT_ID) {
                getSupportFragmentManager().beginTransaction().replace(frameLayoutId, new HomeFragment()).commit();
                return true;
            } else if (id == MUSIC_PLAYER_FRAGMENT_ID) {
                getSupportFragmentManager().beginTransaction().replace(frameLayoutId, new LoadingFragment()).commit();
                return true;
            } else if (id == SETTINGS_FRAGMENT_ID) {
                getSupportFragmentManager().beginTransaction().replace(frameLayoutId, new SettingsFragment()).commit();
                return true;
            } else {
                return false;
            }
        });

        // Set default selected item if no saved instance state
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(HOME_FRAGMENT_ID);
        }
    }
}
