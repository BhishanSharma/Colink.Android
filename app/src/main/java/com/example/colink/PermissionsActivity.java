package com.example.colink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class PermissionsActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_STORAGE_PERMISSIONS = 1001;
    private static final int REQUEST_INSTALL_PACKAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        if (hasStoragePermissions()) {
            initializeFoldersAndProceed();
        } else {
            requestStoragePermissions();
        }
    }

    private boolean hasStoragePermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Storage permissions are required to access files.", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_CODE_STORAGE_PERMISSIONS);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.REQUEST_INSTALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, REQUEST_INSTALL_PACKAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initializeFoldersAndProceed();
            } else {
                Toast.makeText(this, "Storage permissions are required to access files.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initializeFoldersAndProceed() {
        initializeFolders();
        proceedToNextActivity();
    }

    private void initializeFolders() {
        File storage = Environment.getExternalStorageDirectory();
        if (storage != null) {
            File mainFolder = createFolder(storage, getString(R.string.AppDirectory));
            createFolder(mainFolder, getString(R.string.subIDirectory));
            createFolder(mainFolder, getString(R.string.subSDirectory));
            createFolder(mainFolder, getString(R.string.subDDirectory));
            createFolder(mainFolder, getString(R.string.subADirectory));
        }
    }

    private File createFolder(File parent, String folderName) {
        File folder = new File(parent, folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private void proceedToNextActivity() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Intent intent;
        if (currentUser != null) {
            if (checkAvatar()) {
                intent = new Intent(PermissionsActivity.this, HomeActivity.class);
            } else {
                intent = new Intent(PermissionsActivity.this, AvatarActivity.class);
            }
        } else {
            intent = new Intent(PermissionsActivity.this, SignInActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private boolean checkAvatar() {
        File storage = Environment.getExternalStorageDirectory(); // App-specific storage directory
        if (storage != null) {
            String filePath = new File(storage, "CoLink/selected_avatar.png").getAbsolutePath();
            File avatarFile = new File(filePath);
            return avatarFile.exists();
        }
        return false;
    }
}
