package com.example.colink.Tabs;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.colink.AboutActivity;
import com.example.colink.R;
import com.example.colink.SignInActivity;
import com.example.colink.change.avatar;
import com.example.colink.change.name;
import com.example.colink.share.NetworkChangeReceiver;
import com.example.colink.share.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class SettingsFragment extends Fragment {

    private ImageView user_pic;
    private TextView username;
    private LinearLayout change_avatar, change_name, about, log_out;
    private NetworkChangeReceiver networkChangeReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initViews(view);

        // Initial network connectivity check
        if (NetworkUtil.isConnectedToInternet(requireContext())) {
            setClickListeners();
            displayUsername();
        } else {
            waitForNetworkConnection();
        }

        loadSavedAvatarImage();

        return view;
    }

    private void initViews(View view) {
        user_pic = view.findViewById(R.id.user_pic);
        username = view.findViewById(R.id.username);
        change_avatar = view.findViewById(R.id.change_avatar);
        change_name = view.findViewById(R.id.change_name);
        about = view.findViewById(R.id.about);
        log_out = view.findViewById(R.id.log_out);
    }

    private void setClickListeners() {
        change_avatar.setOnClickListener(v -> startActivity(new Intent(requireContext(), avatar.class)));
        change_name.setOnClickListener(v -> startActivity(new Intent(requireContext(), name.class)));
        log_out.setOnClickListener(v -> logOut());
        about.setOnClickListener(v -> startActivity(new Intent(requireContext(), AboutActivity.class)));
    }

    private void displayUsername() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            username.setText(currentUser.getDisplayName());
        }
    }

    private void loadSavedAvatarImage() {
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/CoLink/selected_avatar.png";
        File avatarFile = new File(filePath);

        if (avatarFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            user_pic.setImageBitmap(bitmap);
        } else {
            Toast.makeText(requireContext(), "No avatar image found", Toast.LENGTH_SHORT).show();
        }
    }

    private void logOut() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(requireContext(), SignInActivity.class));
        requireActivity().finish();
    }

    private void waitForNetworkConnection() {
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            if (isConnected) {
                setClickListeners();
                displayUsername();
            } else {
                Toast.makeText(requireContext(), "Waiting for network connection...", Toast.LENGTH_SHORT).show();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        requireContext().registerReceiver(networkChangeReceiver, filter);
    }

    private void unregisterNetworkReceiver() {
        if (networkChangeReceiver != null) {
            requireContext().unregisterReceiver(networkChangeReceiver);
            networkChangeReceiver = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterNetworkReceiver();
    }
}
