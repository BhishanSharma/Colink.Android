package com.example.colink.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.example.colink.R;
import com.example.colink.services.MediaPlayerService;

import java.io.File;
import java.util.ArrayList;

public class UiUtils {
    public static void slideUp(View view, Context context) {
        Animation slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up);
        if (view.getVisibility() == View.GONE) {
            view.setVisibility(View.VISIBLE);
            view.startAnimation(slideUp);
        }
    }

    public static void updateUIWithCurrentSong(Context context, RelativeLayout controls, MediaPlayerService mediaPlayerService, boolean isBound,
                                               ImageView pausePlay, TextView songName, TextView artist, ImageView songBanner,
                                               SeekBar seekBar, Runnable startSeekBarUpdate) {
        File currentFile = mediaPlayerService.getCurrentFile();
        if (!mediaPlayerService.isPlaying()) {
            pausePlay.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.baseline_play_arrow_24, null));
            if (currentFile != null)
                slideUp(controls, context);
        }
        if (isBound && mediaPlayerService != null && mediaPlayerService.isPlaying()) {
            pausePlay.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.baseline_pause_24, null));
            slideUp(controls, context);

            if (currentFile != null)
                songName.setText(currentFile.getName().replace(".mp3", ""));

            artist.setText(mediaPlayerService.getArtist());
            loadImage(context, mediaPlayerService.getImage(), songBanner);
            seekBar.setMax(mediaPlayerService.getDuration());
            startSeekBarUpdate.run();
        }
    }

    public static void loadImage(Context context, byte[] image, ImageView songBanner) {
        try {
            if (image.length > 0) {
                Glide.with(context).asBitmap().load(image).into(songBanner);
            } else {
                Glide.with(context).load(R.drawable.default_album_art).into(songBanner);
            }
        } catch (Exception e) {
            Glide.with(context).load(R.drawable.default_album_art).into(songBanner);
        }
    }

    public static ArrayList<String> toStrALL(ArrayList<File> files) {
        ArrayList<String> converted = new ArrayList<>();
        for (File file : files) {
            converted.add(file.getAbsolutePath());
        }
        return converted;
    }

    public static ArrayList<File> toFiALL(ArrayList<String> paths) {
        ArrayList<File> converted = new ArrayList<>();
        for (String path : paths) {
            converted.add(new File(path));
        }
        return converted;
    }
}
