package com.example.colink.Tabs;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.colink.FolderActivity;
import com.example.colink.Loading.LoadingActivity;
import com.example.colink.R;
import com.example.colink.ReceiveActivity;
import com.example.colink.SelectFilesActivity;
import com.example.colink.share.NetworkChangeReceiver;
import com.example.colink.share.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private ImageView userPic;
    TextView usernameHome;
    FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeFirebase();
        initializeUI(view);
        loadSavedAvatarImage();

        // Initial network connectivity check
        if (NetworkUtil.isConnectedToInternet(requireContext())) {
            displayUsername();
        } else {
            waitForNetworkConnection();
        }

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
    }

    private void initializeUI(View view) {
        userPic = view.findViewById(R.id.user_pic);
        usernameHome = view.findViewById(R.id.username_home);
        Button sendButton = view.findViewById(R.id.sendButton);
        Button receiveButton = view.findViewById(R.id.receiveButton);
        Button folderButton = view.findViewById(R.id.folder);

        setupButtons(folderButton, sendButton, receiveButton);
    }

    private void setupButtons(Button folderButton, Button sendButton, Button receiveButton) {
        folderButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoadingActivity.class);
            intent.putExtra("destination", "Folder");
            startActivity(intent);
        });
        sendButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoadingActivity.class)));
        receiveButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), ReceiveActivity.class)));
    }

    private void loadSavedAvatarImage() {
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/CoLink/selected_avatar.png";
        File avatarFile = new File(filePath);

        if (avatarFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            userPic.setImageBitmap(bitmap);
        } else {
            Toast.makeText(requireContext(), "No avatar image found", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayUsername() {
        if (user != null) {
            if (isDisplayNameEmpty(user)) {
                fetchUserName(user, usernameHome);
            } else {
                usernameHome.setText(user.getDisplayName());
            }
        }
    }

    private boolean isDisplayNameEmpty(FirebaseUser user) {
        String displayName = user.getDisplayName();
        return displayName == null || displayName.trim().isEmpty();
    }

    private void fetchUserName(FirebaseUser user, TextView username) {
        String userId = user.getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        String name = document.getString("name");
                        username.setText(name);
                    } else {
                        handleFetchUserNameError(task.getException());
                    }
                });
    }

    private void handleFetchUserNameError(Exception e) {
        if (e != null) {
            Log.d(TAG, "get failed with ", e);
        }
        Toast.makeText(requireContext(), "Failed to retrieve data", Toast.LENGTH_SHORT).show();
    }

    private void waitForNetworkConnection() {
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            if (isConnected) {
                unregisterNetworkReceiver();
                // Perform actions that require network connectivity
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
