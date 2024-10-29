package com.example.colink;

import static com.example.colink.share.NetworkUtil.getLocalIpAddress;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.colink.Qr.ShowQrActivity;
import com.example.colink.services.FileServerService;

import java.net.SocketException;

public class ReceiveActivity extends AppCompatActivity {

    private FileServerService fileServerService;
    private boolean isBound = false;
    private String IP;
    Button btnDialogCancel, btnDialogAccept;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileServerService.LocalBinder binder = (FileServerService.LocalBinder) service;
            fileServerService = binder.getService();
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
        setContentView(R.layout.activity_receive);

        initializeUI();
    }

    @Override
    public void onBackPressed() {
        stopServiceAndFinish();
        super.onBackPressed();
    }

    private void initializeUI() {
        Button back = findViewById(R.id.back_button);
        ImageView imageView = findViewById(R.id.radar);

        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        imageView.startAnimation(rotateAnimation);

        back.setOnClickListener(v -> {
            stopServiceAndFinish();
        });
    }

    private void initializeService() {
        try {
            IP = getLocalIpAddress();
            if (fileServerService != null) {
                if (!isValidIpAddress(IP)) {
                    showQrConfirmationDialog();
                }
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

    private void stopServiceAndFinish() {
        if (isBound) {
            if (fileServerService != null) {
                fileServerService.stop();
            }
            unbindService(serviceConnection);
            isBound = false;
        }
        finish();
    }

    private boolean isValidIpAddress(String ip) {
        return ip.endsWith(".1");
    }

    private void startQrActivity() {
        Intent intent = new Intent(this, ShowQrActivity.class);
        intent.putExtra("IP", IP);
        startActivity(intent);
    }

    private void showQrConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog_box);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_box_back));
        dialog.setCancelable(false);

        btnDialogAccept = dialog.findViewById(R.id.btnDialogAccept);
        btnDialogCancel = dialog.findViewById(R.id.btnDialogCancel);
        btnDialogAccept.setOnClickListener(v -> {
            startQrActivity();
            dialog.dismiss();
        });
        btnDialogCancel.setOnClickListener(v -> {
            dialog.dismiss();
            stopServiceAndFinish();
        });
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        stopServiceAndFinish();
        super.onDestroy();
    }

    private void showErrorDialog(String title, String message) {
        new Handler(getMainLooper()).post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ReceiveActivity.this);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                    .show();
        });
    }
}
