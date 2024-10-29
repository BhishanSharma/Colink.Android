package com.example.colink;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.colink.Qr.ScanQrActivity;
import com.example.colink.services.FileClientService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;

import static com.example.colink.share.NetworkUtil.getHotspotIpAddress;

public class SendActivity extends AppCompatActivity {
    private String IP;
    private FileClientService fileClientService;
    private boolean isBound = false;
    ImageView scanQr;
    ArrayList<File> selectedItems;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseUser currentUser = mAuth.getCurrentUser();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileClientService.LocalBinder binder = (FileClientService.LocalBinder) service;
            fileClientService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        ImageView receiver = findViewById(R.id.receiver_Server);
        scanQr = findViewById(R.id.scanQr);

        try {
            IP = getHotspotIpAddress();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        selectedItems = (ArrayList<File>) getIntent().getSerializableExtra("selectedItems");

        for (File file : selectedItems) {
            Toast.makeText(this, file.getName(), Toast.LENGTH_SHORT).show();
        }

        receiver.setOnClickListener(v -> {
            if (selectedItems != null && !selectedItems.isEmpty()) {
                Toast.makeText(this, "Sending file to: " + IP, Toast.LENGTH_SHORT).show();
                assert currentUser != null;
                startFileTransfer(selectedItems, currentUser.getDisplayName() + " ", IP);
            } else {
                Toast.makeText(this, "Please select a file first.", Toast.LENGTH_SHORT).show();
            }
        });

        scanQr.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(SendActivity.this);
            integrator.setCaptureActivity(ScanQrActivity.class);
            integrator.setOrientationLocked(false);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan a QR code");
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });

        bindToFileClientService();
    }

    private void bindToFileClientService() {
        Intent intent = new Intent(this, FileClientService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startFileTransfer(ArrayList<File> selectedItems, String name, String IP) {
        if (isBound) {
            fileClientService.upload(IP, getApplicationContext(), selectedItems, name);
        } else {
            Toast.makeText(this, "Service is not bound.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                String scannedIP = result.getContents();
                Toast.makeText(this, "Scanned: " + scannedIP, Toast.LENGTH_LONG).show();
                startFileTransfer(selectedItems, currentUser.getDisplayName(), scannedIP);
            }
        }
    }
}