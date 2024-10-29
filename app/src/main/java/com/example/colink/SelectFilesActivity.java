package com.example.colink;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.colink.Listeners.SelectListener;
import com.example.colink.Adapters.SelectableItemAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class SelectFilesActivity extends AppCompatActivity implements SelectListener {

    private RecyclerView recyclerView;
    private SelectableItemAdapter selectableItemAdapter;
    private ArrayList<File> selectedItems;
    private HashMap<File, Boolean> allPdfs, allSongs, allImages, allApks;

    Button imagesButton, mediaButton, appsButton, docsButton;
    RelativeLayout img_back, music_back, doc_back, app_back;
    RelativeLayout activeButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_files);
        EdgeToEdge.enable(this);

        initializeUI();
        loadFilesInBackground();
        setupButtonListeners();

        displayFiles(allImages);
        setActiveButton(img_back);

    }

    private void initializeUI() {
        selectedItems = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupButtonListeners() {
        imagesButton = findViewById(R.id.images);
        mediaButton = findViewById(R.id.media);
        docsButton = findViewById(R.id.docs);
        appsButton = findViewById(R.id.apps);

        img_back = findViewById(R.id.img_back);
        music_back = findViewById(R.id.music_back);
        doc_back = findViewById(R.id.doc_back);
        app_back = findViewById(R.id.app_back);

        Button backButton = findViewById(R.id.back_button);
        Button submitButton = findViewById(R.id.send_selected);

        backButton.setOnClickListener(v -> finish());
        submitButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendActivity.class);
            intent.putExtra("selectedItems", selectedItems);
            startActivity(intent);
        });

        imagesButton.setOnClickListener(v -> {
            displayFiles(allImages);
            setActiveButton(img_back);
        });
        mediaButton.setOnClickListener(v -> {
            displayFiles(allSongs);
            setActiveButton(music_back);
        });
        docsButton.setOnClickListener(v -> {
            displayFiles(allPdfs);
            setActiveButton(doc_back);
        });
        appsButton.setOnClickListener(v -> {
            displayFiles(allApks);
            setActiveButton(app_back);
        });
    }


    private void setActiveButton(RelativeLayout newActiveButton) {
        if (activeButton != null && activeButton != newActiveButton) {
            resetButton(activeButton);
        }

        newActiveButton.setBackgroundColor(Color.parseColor("#D0C6ED"));
        activeButton = newActiveButton;
    }

    private void resetButton(RelativeLayout activeButton) {
        if (activeButton == findViewById(R.id.img_back)) {
            activeButton.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else if (activeButton == findViewById(R.id.music_back)) {
            activeButton.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else if (activeButton == findViewById(R.id.doc_back)) {
            activeButton.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else if (activeButton == findViewById(R.id.app_back)) {
            activeButton.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
    }


    private void loadFilesInBackground() {
        Intent intent = getIntent();
        Bundle allFilesBundle = intent.getBundleExtra("allFiles");
        allSongs = (HashMap<File, Boolean>) allFilesBundle.getSerializable("allSongs");
        allImages = (HashMap<File, Boolean>) allFilesBundle.getSerializable("allImages");
        allPdfs = (HashMap<File, Boolean>) allFilesBundle.getSerializable("allPdfs");
        allApks = (HashMap<File, Boolean>) allFilesBundle.getSerializable("allApks");
    }

    private void displayFiles(HashMap<File, Boolean> files) {
        if (files != null) {
            selectableItemAdapter = new SelectableItemAdapter(files, getApplicationContext(), this, null);
            runOnUiThread(() -> recyclerView.setAdapter(selectableItemAdapter));
        } else {
            Toast.makeText(this, "Files are still loading, please wait...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemSelect(File item) {
        selectedItems.add(item);
    }

    @Override
    public void onItemDeSelect(File item) {
        selectedItems.remove(item);
    }
}
