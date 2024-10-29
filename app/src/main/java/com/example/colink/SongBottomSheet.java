package com.example.colink;

import static com.example.colink.Utils.FileUtils.*;
import static com.example.colink.Utils.UiUtils.loadImage;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.colink.services.MediaPlayerService;
import com.example.colink.Listeners.SongCompletionListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SongBottomSheet extends BottomSheetDialogFragment implements SongCompletionListener {
    private static final String PREFS_NAME = "LastPlayedSongPrefs";
    private static final String KEY_LAST_SONG = "lastSong";
    private static final String KEY_LAST_POSITION = "lastPosition";

    SeekBar seekBar;
    TextView songName, artist, timeSpent, timeLeft;
    ImageView nextSong, playPause, prevSong, songBanner, backButton;

    private final Handler handler = new Handler();
    private Runnable updateSeekBar;

    BottomSheetListener listener;
    BottomSheetBehavior<View> bottomSheetBehavior;

    MediaPlayerService mediaPlayerService;
    private boolean isBound = false;

    ArrayList<String> songs = new ArrayList<>();
    private String currSongPath;
    int position;

    enum Move {NEXT, PREV}

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

    private void updateUI(String R_songName, String artist, byte[] image, boolean isPlaying) {
        songName.setText(R_songName);
        this.artist.setText(artist);
        loadImage(requireContext(), image, songBanner);
        playPause.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24, null));
        if (isPlaying) {
            updateSeekBarAndTime(isPlaying);
        } else {
            handler.removeCallbacks(updateSeekBar);
        }
    }

    public static SongBottomSheet newInstance(ArrayList<String> songs, String currSongPath, int position) {
        SongBottomSheet fragment = new SongBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList("songs", songs);
        args.putString("currSongPath", currSongPath);
        args.putInt("position", position);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_song_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fullScreenMode(view);
        retrieveArgs();
        initialiseViews(view);
        setUpClickListeners();
        setSeekBarChangeListener();
    }

    private void fullScreenMode(View view) {
        bottomSheetBehavior = (BottomSheetBehavior.from((View) view.getParent()));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        RelativeLayout rl = view.findViewById(R.id.BottomSheet);
        rl.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
    }

    private void initialiseViews(View view) {
        backButton = view.findViewById(R.id.back_button_sp);
        seekBar = view.findViewById(R.id.seekbar_sp);
        songName = view.findViewById(R.id.song_name_sp);
        artist = view.findViewById(R.id.artist_sp);
        timeSpent = view.findViewById(R.id.time_spent_sp);
        timeLeft = view.findViewById(R.id.time_left_sp);
        nextSong = view.findViewById(R.id.next_button_sp);
        prevSong = view.findViewById(R.id.prev_button_sp);
        playPause = view.findViewById(R.id.play_pause_button_sp);
        songBanner = view.findViewById(R.id.song_banner_sp);
    }

    private void retrieveArgs() {
        if (getArguments() != null) {
            songs = getArguments().getStringArrayList("songs");
            currSongPath = getArguments().getString("currSongPath");
            position = getArguments().getInt("position");
        }
    }

    private void setUpClickListeners() {
        backButton.setOnClickListener(v -> dismiss());
        playPause.setOnClickListener(v -> handlePlaybackAction(null));
        nextSong.setOnClickListener(v -> handlePlaybackAction(Move.NEXT));
        prevSong.setOnClickListener(v -> handlePlaybackAction(Move.PREV));
    }

    private void setSeekBarChangeListener() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && mediaPlayerService != null) {
                    mediaPlayerService.seekTo(progress);
                    updateTimeViews(progress, seekBar.getMax());
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
    }

    private void handlePlaybackAction(@Nullable Move move) {
        if (isBound && mediaPlayerService != null) {
            if (move != null) {
                // Handle song move (next/prev)
                if (listener != null) {
                    listener.onButtonClicked("update");
                }
                if (!mediaPlayerService.isPlaying()) {
                    playPause.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.pause));
                    listener.onButtonClicked("play");
                }
                switch (move) {
                    case NEXT:
                        mediaPlayerService.nextSong();
                        break;
                    case PREV:
                        mediaPlayerService.prevSong();
                        break;
                }
                saveLastPlayedSong(mediaPlayerService.getCurrentFile(), mediaPlayerService.getCurrentPosition(), requireContext(), PREFS_NAME, KEY_LAST_SONG, KEY_LAST_SONG);
            } else {
                // Handle pause/play
                if (mediaPlayerService.isPlaying()) {
                    if (listener != null) listener.onButtonClicked("pause");
                    mediaPlayerService.pause();
                    playPause.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.play));
                    handler.removeCallbacks(updateSeekBar);
                } else {
                    if (listener != null) listener.onButtonClicked("play");
                    updateUIWithCurrentSong();
                    mediaPlayerService.resume();
                    playPause.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.pause));
                    handler.post(updateSeekBar);
                }
            }
            updateUIWithCurrentSong();
        }
    }

    // -- //
    private void updateUIWithCurrentSong() {
        if (isBound && mediaPlayerService != null) {
            boolean isPlaying = mediaPlayerService.isPlaying();
            updatePlayPauseButton(isPlaying);
            updateSongDetails(isPlaying);
            updateSeekBarAndTime(isPlaying);
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        int drawableId = isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24;
        playPause.setImageDrawable(ResourcesCompat.getDrawable(getResources(), drawableId, null));
    }

    private void updateSongDetails(boolean isPlaying) {
        if (isBound && mediaPlayerService != null) {
            File currentFile = mediaPlayerService.getCurrentFile();
            songName.setText(currentFile != null ? currentFile.getName().replace(".mp3", "") : "");
            artist.setText(mediaPlayerService.getArtist());
            loadImage(requireContext(), mediaPlayerService.getImage(), songBanner);
        } else if (currSongPath != null) {
            File song = new File(currSongPath);
            songName.setText(song.getName().replace(".mp3", ""));
            artist.setText(getArtistName(song.getAbsolutePath()));
            loadImage(requireContext(), getAlbumArt(Uri.fromFile(song), requireContext()), songBanner);
        }
    }

    private void updateSeekBarAndTime(boolean isPlaying) {
        seekBar.setMax(mediaPlayerService.getDuration());

        if (updateSeekBar == null) {
            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (isBound && mediaPlayerService != null && mediaPlayerService.isPlaying()) {
                        int currentPosition = mediaPlayerService.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                        updateTimeViews(currentPosition, mediaPlayerService.getDuration());
                        handler.postDelayed(this, 1000);
                    }
                }
            };
        }
        handler.post(updateSeekBar);

        if (!isPlaying) {
            updateTimeViews(mediaPlayerService.getCurrentPosition(), mediaPlayerService.getDuration());
        }
    }
    // -- //

    private void updateTimeViews(int currentPosition, int duration) {
        timeSpent.setText(formatTime(currentPosition));
        timeLeft.setText(formatTime(duration - currentPosition));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacks(updateSeekBar);
        requireContext().unregisterReceiver(updateReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
        requireContext().bindService(new Intent(getContext(), MediaPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter(MediaPlayerService.ACTION_UPDATE_UI);
        requireContext().registerReceiver(updateReceiver, filter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BottomSheetListener) {
            listener = (BottomSheetListener) context;
        } else {
            Fragment targetFragment = getTargetFragment();
            if (targetFragment instanceof BottomSheetListener) {
                listener = (BottomSheetListener) targetFragment;
            } else {
                throw new ClassCastException(context + " or target fragment must implement BottomSheetListener");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener.onButtonClicked("update");
    }

    private void setLastPlayedSong() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currSongPath = sharedPreferences.getString(KEY_LAST_SONG, null);
    }

    public interface BottomSheetListener {
        void onButtonClicked(String data);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            mediaPlayerService.setSongCompletionListener(SongBottomSheet.this);
            isBound = true;
            if (!mediaPlayerService.isPlaying()) {
                setLastPlayedSong();
            }
            updateUIWithCurrentSong();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public void completed() {
        if (isBound && mediaPlayerService != null) {
            try {
                updateUIWithCurrentSong();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}