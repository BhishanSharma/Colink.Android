package com.example.colink.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.colink.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static byte[] getAlbumArt(Uri uri, Context context) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(context, uri);
            byte[] art = retriever.getEmbeddedPicture();
            return art != null ? art : new byte[0];
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving album art: " + e.getMessage(), e);
            return new byte[0];
        }
    }

    public static String getArtistName(String filePath) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(filePath);
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static String formatTime(int millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    public static void saveLastPlayedSong(File file, int position, Context context, String PREFS_NAME, String KEY_LAST_SONG, String KEY_LAST_POSITION) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_SONG, file.getAbsolutePath());
        editor.putInt(KEY_LAST_POSITION, position);
        editor.apply();
    }
}
