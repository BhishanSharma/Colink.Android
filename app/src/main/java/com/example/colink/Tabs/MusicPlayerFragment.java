package com.example.colink.Tabs;

import static com.example.colink.Utils.FileUtils.*;
import static com.example.colink.Utils.UiUtils.*;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.colink.Listeners.ClickListener;
import com.example.colink.Adapters.SongsAdapter;
import com.example.colink.SongBottomSheet;
import com.example.colink.services.MediaPlayerService;
import com.example.colink.Listeners.SongCompletionListener;
import com.example.colink.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MusicPlayerFragment extends Fragment implements ClickListener, SongCompletionListener, SongBottomSheet.BottomSheetListener {
    private static final String PREFS_NAME = "LastPlayedSongPrefs";
    private static final String KEY_LAST_SONG = "lastSong";
    private static final String KEY_LAST_POSITION = "lastPosition";

    String songPath;
    RecyclerView recyclerView;
    HashMap<File, Boolean> allSongs;
    SongsAdapter songsAdapter;
    RelativeLayout controls;
    ImageView songBanner;
    TextView song_name;
    TextView artist;
    int position;
    ImageView pausePlay;
    SeekBar seekBar;
    Handler handler = new Handler();
    Runnable updateSeekBar;

    MediaPlayerService mediaPlayerService;
    boolean isBound = false;

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

    private void updateUI(String songName, String artist, byte[] image, boolean isPlaying) {
        song_name.setText(songName);
        this.artist.setText(artist);
        loadImage(requireContext(), image, songBanner);
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
            mediaPlayerService.setSongCompletionListener(MusicPlayerFragment.this);

            if (!mediaPlayerService.isPlaying()) {
                pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_play_arrow_24, null));
            } else {
                pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_pause_24, null));
            }

            updateUIWithCurrentSong(requireContext(), controls, mediaPlayerService, isBound,
                    pausePlay, song_name, artist, songBanner, seekBar, MusicPlayerFragment.this::startSeekBarUpdate);

            if (!mediaPlayerService.isPlaying()) {
                setLastPlayedSong();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_music_player, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        getParentFragmentManager().setFragmentResultListener("dataFromLoader", this, (requestKey, result) -> {
            @SuppressWarnings("unchecked")
            HashMap<File, Boolean> receivedSongs = (HashMap<File, Boolean>) result.getSerializable("allSongs");
            if (receivedSongs != null) {
                allSongs = receivedSongs;
                displayFiles(allSongs);
            } else {
                Toast.makeText(requireContext(), "No songs received", Toast.LENGTH_SHORT).show();
            }
        });

        controls = view.findViewById(R.id.controls);
        songBanner = view.findViewById(R.id.song_banner);

        song_name = view.findViewById(R.id.songName);
        song_name.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        song_name.setSelected(true);

        artist = view.findViewById(R.id.artist);
        pausePlay = view.findViewById(R.id.pause_play_button);
        seekBar = view.findViewById(R.id.progressbar);

        controls.setOnClickListener(v -> {
            ArrayList<String> songPaths = new ArrayList<>();
            for (File song : allSongs.keySet()) {
                songPaths.add(song.getAbsolutePath());
            }
            SongBottomSheet songBottomSheet = SongBottomSheet.newInstance(songPaths, mediaPlayerService.getCurrentFile().getAbsolutePath(), position);
            songBottomSheet.setTargetFragment(MusicPlayerFragment.this, 0); // Set this Fragment as the target
            songBottomSheet.show(getParentFragmentManager(), songBottomSheet.getTag());
        });

        pausePlay.setOnClickListener(v -> {
            if (isBound && mediaPlayerService != null) {
                if (mediaPlayerService.isPlaying()) {
                    mediaPlayerService.pause();
                    pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_play_arrow_24, null));
                    handler.removeCallbacks(updateSeekBar);
                } else {
                    mediaPlayerService.resume();
                    pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_pause_24, null));
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
                if (isBound && mediaPlayerService != null) {
                    handler.removeCallbacks(updateSeekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && mediaPlayerService != null) {
                    handler.post(updateSeekBar);
                }
            }
        });

        return view;
    }

    private void displayFiles(HashMap<File, Boolean> files) {
        if (files != null && !files.isEmpty()) {
            songsAdapter = new SongsAdapter(files, this);
            recyclerView.setAdapter(songsAdapter);
        } else {
            Toast.makeText(requireContext(), "No songs found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(File R_file, int R_position, String R_song_name, String R_artist_name, byte[] image) {
        allSongs.put(R_file, true);
        slideUp(controls, requireContext());
        song_name.setText(R_song_name.replace(".mp3", ""));
        artist.setText(R_artist_name);
        position = R_position;
        songPath = R_file.getAbsolutePath();

        loadImage(requireContext(), image, songBanner);

        if (isBound && mediaPlayerService != null) {
            mediaPlayerService.play(R_file, new ArrayList<>(allSongs.keySet()), R_position, R_artist_name, image, requireContext());
            seekBar.setMax(mediaPlayerService.getDuration());
            startSeekBarUpdate();
            pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_pause_24, null));
            saveLastPlayedSong(R_file, R_position, requireContext(), PREFS_NAME, KEY_LAST_SONG, KEY_LAST_POSITION);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(requireContext(), MediaPlayerService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter(MediaPlayerService.ACTION_UPDATE_UI);
        requireContext().registerReceiver(updateReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isBound && mediaPlayerService != null) {
            updateUIWithCurrentSong(requireContext(), controls, mediaPlayerService, isBound,
                    pausePlay, song_name, artist, songBanner, seekBar, MusicPlayerFragment.this::startSeekBarUpdate);
        } else {
            Intent intent = new Intent(requireContext(), MediaPlayerService.class);
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBound) {
//            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacks(updateSeekBar);
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
        if (isBound && mediaPlayerService != null) {
            updateUIWithCurrentSong(requireContext(), controls, mediaPlayerService, isBound,
                    pausePlay, song_name, artist, songBanner, seekBar, MusicPlayerFragment.this::startSeekBarUpdate);
        }
    }

    private void setLastPlayedSong() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastSongPath;
        try {
            lastSongPath = sharedPreferences.getString(KEY_LAST_SONG, null);
        } catch (ClassCastException e) {
            System.out.println(e.getMessage());
            return;
        }

        int lastPosition = sharedPreferences.getInt(KEY_LAST_POSITION, -1);

        if (lastSongPath != null && lastPosition != -1) {
            File lastSongFile = new File(lastSongPath);
            if (allSongs != null && allSongs.containsKey(lastSongFile)) {
                onClick(lastSongFile, lastPosition, lastSongFile.getName(), getArtistName(lastSongFile.getAbsolutePath()), getAlbumArt(Uri.fromFile(lastSongFile), requireContext())); // Assuming artist name and image can be empty or null
                songPath = lastSongFile.getAbsolutePath();
            }
        }

        mediaPlayerService.pause();
        pausePlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_play_arrow_24, null));
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
                updateUIWithCurrentSong(requireContext(), controls, mediaPlayerService, isBound,
                        pausePlay, song_name, artist, songBanner, seekBar, MusicPlayerFragment.this::startSeekBarUpdate);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(updateReceiver);
    }
}