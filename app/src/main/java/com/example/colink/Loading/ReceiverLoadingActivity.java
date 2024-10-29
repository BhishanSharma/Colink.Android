package com.example.colink.Loading;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.colink.Adapters.SelectableItemAdapter;
import com.example.colink.R;
import com.example.colink.services.FileServerService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ReceiverLoadingActivity extends AppCompatActivity implements FileServerService.GetReceiveFile {

    private final ArrayList<File> receivedFiles = new ArrayList<>();
    private FileServerService fileServerService;
    private boolean isBound = false;
    private TextView fileCountTV;
    private SeekBar seekBarSB;
    private int fileCount;
    private RecyclerView recyclerView;
    private SelectableItemAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                FileServerService.LocalBinder binder = (FileServerService.LocalBinder) service;
                fileServerService = binder.getService();
                fileServerService.setGetReceiveFileListener(ReceiverLoadingActivity.this);
                isBound = true;
            } catch (Exception e) {
                Toast.makeText(ReceiverLoadingActivity.this, "Error in onServiceConnected: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_loading);

        try {
            fileCountTV = findViewById(R.id.filecount);
            seekBarSB = findViewById(R.id.seekbar);

            Intent intent = getIntent();
            fileCount = intent.getIntExtra("fileCount", 0);
            updateFileCountText();

            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SelectableItemAdapter(new HashMap<>(), this, null, null);
            recyclerView.setAdapter(adapter);

            bindToFileServerService();
        } catch (Exception e) {
            Toast.makeText(this, "Error in onCreate: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void bindToFileServerService() {
        try {
            Intent intent = new Intent(this, FileServerService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Toast.makeText(this, "Error binding to service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (isBound) {
                unbindService(serviceConnection);
                isBound = false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error in onDestroy: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void updateProgress(File file) {
        mainHandler.post(() -> {
            try {
                receivedFiles.add(file);
                updateUI();
            } catch (Exception e) {
                Toast.makeText(this, "Error in updateProgress: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUI() {
        mainHandler.post(() -> {
            try {
                updateFileCountText();
                if (fileCount > 0) {
                    int progress = (receivedFiles.size() * 100) / fileCount;
                    seekBarSB.setProgress(progress);
                    displayFiles();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error in updateUI: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateFileCountText() {
        try {
            fileCountTV.setText(String.format("%d/%d", receivedFiles.size(), fileCount));
        } catch (Exception e) {
            Toast.makeText(this, "Error in updateFileCountText: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void displayFiles() {
        try {
            if (!receivedFiles.isEmpty()) {
                adapter.updateData(convertListToMap(receivedFiles));
            } else {
                Toast.makeText(this, "No files found.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error in displayFiles: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private HashMap<File, Boolean> convertListToMap(ArrayList<File> fileList) {
        try {
            return fileList.stream().collect(Collectors.toMap(file -> file, file -> false, (e1, e2) -> e1, HashMap::new));
        } catch (Exception e) {
            Toast.makeText(this, "Error in convertListToMap: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new HashMap<>();
        }
    }
}
