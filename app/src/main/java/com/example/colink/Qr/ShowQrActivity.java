package com.example.colink.Qr;

import static com.example.colink.share.NetworkUtil.getLocalIpAddress;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.colink.R;
import com.example.colink.services.FileServerService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.net.SocketException;

public class ShowQrActivity extends AppCompatActivity {

    private FileServerService fileServerService;
    private boolean isBound = false;
    private String IP;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileServerService.LocalBinder binder = (FileServerService.LocalBinder) service;
            fileServerService = binder.getService();
            fileServerService.stop();
            isBound = true;
            initializeService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            fileServerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_show);

        Intent intent = getIntent();
        IP = intent.getStringExtra("IP");

        ImageView qr = findViewById(R.id.qr);
        Toast.makeText(this, "IP: " + IP, Toast.LENGTH_SHORT).show();

        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(IP, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(matrix);
            qr.setImageBitmap(bitmap);
        } catch (WriterException e) {
            showErrorDialog("Error", "Unable to generate QR code.");
        }
    }

    private void initializeService() {
        try {
            IP = getLocalIpAddress();
            if (fileServerService != null) {
                fileServerService.start(IP, getApplicationContext());
            }
        } catch (SocketException e) {
            showErrorDialog("Error", "Unable to get local IP address. Please try again.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, FileServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBound) {
            stopServiceAndFinish();
        }
    }

    private void stopServiceAndFinish() {
        if (isBound && fileServerService != null) {
            fileServerService.stop();
            unbindService(serviceConnection);
            isBound = false;
        }
        finish();
    }

    private void showErrorDialog(String title, String message) {
        new Handler(getMainLooper()).post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ShowQrActivity.this);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                    .show();
        });
    }
}
