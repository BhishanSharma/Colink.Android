package com.example.colink.Loading;

import static com.example.colink.Utils.UiUtils.toStrALL;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.colink.FolderActivity;
import com.example.colink.R;
import com.example.colink.SelectFilesActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LoadingActivity extends AppCompatActivity {
    private final HashMap<File, Boolean> allPdfs = new HashMap<>();
    private final HashMap<File, Boolean> allSongs = new HashMap<>();
    private final HashMap<File, Boolean> allImages = new HashMap<>();
    private final HashMap<File, Boolean> allApks = new HashMap<>();
    ArrayList<File> images = new ArrayList<>();
    ArrayList<File> songs = new ArrayList<>();
    ArrayList<File> pdfs = new ArrayList<>();
    ArrayList<File> apks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        Intent intent = getIntent();
        String destination = intent.getStringExtra("destination");

        if ("Folder".equals(destination)) {
            loadFilesForFolder();
        } else {
            loadFilesInBackground();
        }

    }

    private void loadFilesForFolder() {
        File mainFolder = new File(Environment.getExternalStorageDirectory(), getString(R.string.AppDirectory));
        loadFolder(mainFolder, getString(R.string.subIDirectory), images);
        loadFolder(mainFolder, getString(R.string.subSDirectory), songs);
        loadFolder(mainFolder, getString(R.string.subDDirectory), pdfs);
        loadFolder(mainFolder, getString(R.string.subADirectory), apks);
        ArrayList<String> imagesP = toStrALL(images);
        ArrayList<String> songsP = toStrALL(songs);
        ArrayList<String> pdfsP = toStrALL(pdfs);
        ArrayList<String> apksP = toStrALL(apks);

        Intent intent = new Intent(this, FolderActivity.class);
        intent.putStringArrayListExtra("images", imagesP);
        intent.putStringArrayListExtra("songs", songsP);
        intent.putStringArrayListExtra("pdfs", pdfsP);
        intent.putStringArrayListExtra("apks", apksP);
        startActivity(intent);
        finish();
    }

    private void loadFolder(File mainFolder, String subFolder, ArrayList<File> fileList) {
        File folder = new File(mainFolder, subFolder);
        File[] files = folder.listFiles();
        if (files != null) {
            fileList.addAll(Arrays.asList(files));
        } else {
            Toast.makeText(this, "Failed to load files from " + subFolder, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFilesInBackground() {
        File storageDir = Environment.getExternalStorageDirectory();
        new Thread(() -> {
            loadFiles(storageDir, new String[]{".png", ".jpg", ".jpeg"}, allImages);
            loadFiles(storageDir, new String[]{".mp3"}, allSongs);
            loadFiles(storageDir, new String[]{".pdf"}, allPdfs);
            loadInstalledApks();
            runOnUiThread(this::onDataLoaded);
        }).start();
    }

    private void loadInstalledApks() {
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : installedApps) {
            if (!isSystemApp(appInfo)) {
                File apkFile = new File(appInfo.sourceDir);
                allApks.put(apkFile, false);
            }
        }
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private void loadFiles(File directory, String[] extensions, HashMap<File, Boolean> fileList) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isHidden() && file.isDirectory()) {
                    loadFiles(file, extensions, fileList);
                } else {
                    for (String ext : extensions) {
                        if (file.getName().toLowerCase().endsWith(ext) && !file.getName().startsWith(".")) {
                            fileList.put(file, false);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void onDataLoaded() {
        if (allSongs != null && !allSongs.isEmpty()) {
            Bundle result = new Bundle();
            result.putSerializable("allSongs", allSongs);
            result.putSerializable("allImages", allImages);
            result.putSerializable("allPdfs", allPdfs);
            result.putSerializable("allApks", allApks);

            Intent intent = new Intent(getApplicationContext(), SelectFilesActivity.class);
            intent.putExtra("allFiles", result);
            startActivity(intent);
            finish();
        }
    }
}
