package com.example.colink;

import static com.example.colink.Utils.FileUtils.*;
import static com.example.colink.Utils.UiUtils.*;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.colink.Listeners.ClickListener;
import com.example.colink.Listeners.openListener;
import com.example.colink.services.MediaPlayerService;
import com.example.colink.Adapters.SelectableItemAdapter;
import com.example.colink.Listeners.SongCompletionListener;
import com.example.colink.Adapters.SongsAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class FolderActivity extends AppCompatActivity implements ClickListener, SongCompletionListener, SongBottomSheet.BottomSheetListener, openListener {
    private RecyclerView recyclerView;
    private ArrayList<File> images = new ArrayList<>();
    private ArrayList<File> songs = new ArrayList<>();
    private ArrayList<File> pdfs = new ArrayList<>();
    private ArrayList<File> apks = new ArrayList<>();
    private final String AUTHORITY = "com.example.colink.fileprovider";

    private static final String PREFS_NAME = "LastPlayedSongPrefs";
    private static final String KEY_LAST_SONG = "lastSong";
    private static final String KEY_LAST_POSITION = "lastPosition";

    private RelativeLayout controls;
    private ImageView songBanner;
    private TextView songName, artist;
    private ImageView pausePlay;
    private SeekBar seekBar;
    private final Handler handler = new Handler();
    private Runnable updateSeekBar;
    int position;

    Button imagesButton, mediaButton, appsButton, docsButton;
    RelativeLayout img_back, music_back, doc_back, app_back;
    private RelativeLayout activeButton = null;

    private MediaPlayerService mediaPlayerService;
    private boolean isBound = false;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MediaPlayerService.ACTION_UPDATE_UI.equals(intent.getAction())) {
                boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                String currentFile = intent.getStringExtra("currentFile");
                String artist = intent.getStringExtra("artist");
                byte[] image = intent.getByteArrayExtra("image");

                // Update UI with the received data
                updateUI(currentFile, artist, image, isPlaying);
            }
        }
    };

    private void updateUI(String RsongName, String artist, byte[] image, boolean isPlaying) {
        songName.setText(RsongName);
        this.artist.setText(artist);
        loadImage(getApplicationContext(), image, songBanner);
        pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24, null));
        if (isPlaying) {
            startSeekBarUpdate();
        } else {
            handler.removeCallbacks(updateSeekBar);
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            isBound = true;
            mediaPlayerService.setSongCompletionListener(FolderActivity.this);

            updateUIWithCurrentSong(getApplicationContext(), controls, mediaPlayerService, isBound,
                    pausePlay, songName, artist, songBanner, seekBar, FolderActivity.this::startSeekBarUpdate);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        initializeUI();
        setupRecyclerView();
        loadFiles();
        setupControls();
    }

    private void initializeUI() {
        controls = findViewById(R.id.controls);
        songBanner = findViewById(R.id.song_banner);
        songName = findViewById(R.id.songName);
        songName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songName.setSelected(true);

        artist = findViewById(R.id.artist);
        pausePlay = findViewById(R.id.pause_play_button);
        seekBar = findViewById(R.id.progressbar);

        imagesButton = findViewById(R.id.images);
        mediaButton = findViewById(R.id.media);
        docsButton = findViewById(R.id.docs);
        appsButton = findViewById(R.id.apps);

        img_back = findViewById(R.id.img_back);
        music_back = findViewById(R.id.music_back);
        doc_back = findViewById(R.id.doc_back);
        app_back = findViewById(R.id.app_back);

        Button backButton = findViewById(R.id.folder_back_button);

        imagesButton.setOnClickListener(v -> {
            displayFiles(images);
            setActiveButton(img_back);
        });

        mediaButton.setOnClickListener(v -> {
            displayFiles(songs);
            setActiveButton(music_back);
        });

        docsButton.setOnClickListener(v -> {
            displayFiles(pdfs);
            setActiveButton(doc_back);
        });

        appsButton.setOnClickListener(v -> {
            displayFiles(apks);
            setActiveButton(app_back);
        });

        backButton.setOnClickListener(v -> finish());
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

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadFiles() {
        Intent intent = getIntent();
        images = toFiALL(Objects.requireNonNull(intent.getStringArrayListExtra("images")));
        songs = toFiALL(Objects.requireNonNull(intent.getStringArrayListExtra("songs")));
        pdfs = toFiALL(Objects.requireNonNull(intent.getStringArrayListExtra("pdfs")));
        apks = toFiALL(Objects.requireNonNull(intent.getStringArrayListExtra("apks")));

        if (!images.isEmpty()) {
            displayFiles(images);
            setActiveButton(img_back);
        } else if (!songs.isEmpty()) {
            displayFiles(songs);
            setActiveButton(music_back);
        } else if (!pdfs.isEmpty()) {
            displayFiles(pdfs);
            setActiveButton(doc_back);
        } else {
            displayFiles(apks);
            setActiveButton(app_back);
        }
    }

    private void setupControls() {
        controls.setOnClickListener(v -> {
            ArrayList<String> songPaths = new ArrayList<>();
            for (File song : songs) {
                songPaths.add(song.getAbsolutePath());
            }

            SongBottomSheet songBottomSheet = SongBottomSheet.newInstance(songPaths, songName.getText().toString(), position);
            songBottomSheet.show(getSupportFragmentManager(), songBottomSheet.getTag());
        });

        pausePlay.setOnClickListener(v -> {
            if (isBound && mediaPlayerService != null) {
                if (mediaPlayerService.isPlaying()) {
                    mediaPlayerService.pause();
                    pausePlay.setImageResource(R.drawable.baseline_play_arrow_24);
                    handler.removeCallbacks(updateSeekBar);
                } else {
                    mediaPlayerService.resume();
                    pausePlay.setImageResource(R.drawable.baseline_pause_24);
                    handler.post(updateSeekBar);
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && mediaPlayerService != null) {
                    mediaPlayerService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.post(updateSeekBar);
            }
        });
    }

    private void displayFiles(ArrayList<File> files) {
        if (files == images || files == pdfs || files == apks) {
            SelectableItemAdapter selectableItemAdapter = new SelectableItemAdapter(convertListToMap(files), getApplicationContext(), null, this);
            recyclerView.setAdapter(selectableItemAdapter);
        } else {
            SongsAdapter songsAdapter = new SongsAdapter(convertListToMap(files), this);
            recyclerView.setAdapter(songsAdapter);
        }
    }

    private HashMap<File, Boolean> convertListToMap(ArrayList<File> fileList) {
        return fileList.stream().collect(Collectors.toMap(file -> file, file -> false, (e1, e2) -> e1, HashMap::new));
    }

    @Override
    public void onClick(File file, int position, String songName, String artistName, byte[] image) {
        slideUp(controls, this);
        this.songName.setText(songName.replace(".mp3", ""));
        artist.setText(artistName);
        this.position = position;
        loadImage(getApplicationContext(), image, songBanner);
        pausePlay.setImageResource(R.drawable.baseline_pause_24);

        if (isBound && mediaPlayerService != null) {
            mediaPlayerService.play(file, new ArrayList<>(songs), position, artistName, image, getApplicationContext());
            seekBar.setMax(mediaPlayerService.getDuration());
            startSeekBarUpdate();
            saveLastPlayedSong(file, position, getApplicationContext(), PREFS_NAME, KEY_LAST_SONG, KEY_LAST_POSITION);
        }
    }

    private void startSeekBarUpdate() {
        updateSeekBar = () -> {
            if (isBound && mediaPlayerService != null && mediaPlayerService.isPlaying()) {
                seekBar.setProgress(mediaPlayerService.getCurrentPosition());
                handler.postDelayed(updateSeekBar, 1000);
            }
        };
        handler.post(updateSeekBar);
    }

    @Override
    public void completed() {
        updateUIWithCurrentSong(this, controls, mediaPlayerService, isBound,
                pausePlay, songName, artist, songBanner, seekBar, this::startSeekBarUpdate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter(MediaPlayerService.ACTION_UPDATE_UI);
        getApplicationContext().registerReceiver(updateReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onButtonClicked(String data) {
        switch (data) {
            case "pause":
                pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_play_arrow_24, null));
                break;
            case "play":
                pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_pause_24, null));
                break;
            case "update":
                updateUIWithCurrentSong(this, controls, mediaPlayerService, isBound,
                        pausePlay, songName, artist, songBanner, seekBar, this::startSeekBarUpdate);
        }
    }

    @Override
    public void open(File file) {
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this, AUTHORITY, file);
        } else {
            uri = Uri.fromFile(file);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant temporary read permission

        if (file.getName().endsWith("png") || file.getName().endsWith("jpg") || file.getName().endsWith("jpeg")) {
            intent.setDataAndType(uri, "image/*");
            startActivity(intent);
        } else if (file.getName().endsWith("pdf")) {
            intent.setDataAndType(uri, "application/pdf");
            startActivity(intent);
        } else if (file.getName().endsWith("apk")) {
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            startActivity(intent);
        } else {
            Toast.makeText(this, "Unsupported file type", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(MediaPlayerService.ACTION_UPDATE_UI);
        getApplicationContext().registerReceiver(updateReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getApplicationContext().unregisterReceiver(updateReceiver);
    }
}