package com.example.colink.services;

import static com.example.colink.Utils.FileUtils.getAlbumArt;
import static com.example.colink.Utils.FileUtils.getArtistName;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.example.colink.Listeners.SongCompletionListener;
import com.example.colink.R;

import java.io.File;
import java.util.ArrayList;

public class MediaPlayerService extends Service {
    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private File currentFile;
    private String artist;
    private byte[] image;
    private ArrayList<File> songsList;
    private int position;
    private SongCompletionListener complete;
    private Context context;
    private static final String CHANNEL_ID = "MediaPlayerServiceChannel";
    public static final String ACTION_PLAY = "com.example.colink.action.PLAY";
    public static final String ACTION_PAUSE = "com.example.colink.action.PAUSE";
    public static final String ACTION_NEXT = "com.example.colink.action.NEXT";
    public static final String ACTION_PREV = "com.example.colink.action.PREV";
    public static final String ACTION_UPDATE_UI = "com.example.colink.action.UPDATE_UI";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void sendUpdateBroadcast() {
        Intent intent = new Intent(ACTION_UPDATE_UI);
        intent.putExtra("isPlaying", isPlaying());
        intent.putExtra("currentFile", currentFile.getName());
        intent.putExtra("artist", artist);
        intent.putExtra("image", image);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Player Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(boolean isPlaying) {
        Intent playIntent = new Intent(this, MediaPlayerService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent(this, MediaPlayerService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, MediaPlayerService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent prevIntent = new Intent(this, MediaPlayerService.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Drawable smallDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.app_icon, null);
        Bitmap smallIcon = null;
        if (smallDrawable instanceof BitmapDrawable) {
            smallIcon = ((BitmapDrawable) smallDrawable).getBitmap();
        }

        Bitmap songImage = null;
        if (image != null) {
            songImage = BitmapFactory.decodeByteArray(image, 0, image.length);
        } else {
            Drawable largeDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.default_album_art, null);
            if (largeDrawable instanceof BitmapDrawable) {
                songImage = ((BitmapDrawable) largeDrawable).getBitmap();
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentFile.getName().replace(".mp3", ""))
                .setContentText(artist)
                .setSmallIcon(IconCompat.createWithBitmap(smallIcon))
                .setLargeIcon(songImage)
                .addAction(R.drawable.baseline_previous_24, "Previous", prevPendingIntent)
                .addAction(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24, isPlaying ? "Pause" : "Play", isPlaying ? pausePendingIntent : playPendingIntent)
                .addAction(R.drawable.baseline_next_24, "Next", nextPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying);

        if (!isPlaying) {
            builder.setAutoCancel(true);
        }

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                resume();
            } else if (ACTION_PAUSE.equals(action)) {
                pause();
            } else if (ACTION_NEXT.equals(action)) {
                nextSong();
            } else if (ACTION_PREV.equals(action)) {
                prevSong();
            }
        }
        return START_NOT_STICKY;
    }

    public void setSongCompletionListener(SongCompletionListener listener) {
        this.complete = listener;
    }

    private void notifySongCompleted() {
        if (complete != null) {
            complete.completed();
        }
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void play(File file, ArrayList<File> songs, int position, String artistName, byte[] imageBytes, Context context) {
        currentFile = file;
        artist = artistName;
        image = imageBytes;
        songsList = songs;
        this.position = position;
        this.context = context;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(file));

        mediaPlayer.setOnCompletionListener(mp -> {
            nextSong();
            notifySongCompleted();
        });

        mediaPlayer.start();
        startForeground(1, getNotification(true));
        sendUpdateBroadcast();
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            stopForeground(false);
            updateNotification();
            sendUpdateBroadcast();
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            startForeground(1, getNotification(true));
            sendUpdateBroadcast();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, getNotification(isPlaying()));
        }
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public String getArtist() {
        return artist;
    }

    public byte[] getImage() {
        return image;
    }

    public void nextSong() {
        position = (position + 1) % songsList.size();
        File nextSong = songsList.get(position);
        String artistNameN = getArtistName(nextSong.getAbsolutePath());
        byte[] imageN = getAlbumArt(Uri.fromFile(nextSong), context);

        play(nextSong, songsList, position, artistNameN, imageN, context);
        sendUpdateBroadcast();
    }

    public void prevSong() {
        position = (position - 1 + songsList.size()) % songsList.size();
        File prevSong = songsList.get(position);
        String artistNameP = getArtistName(prevSong.getAbsolutePath());
        byte[] imageP = getAlbumArt(Uri.fromFile(prevSong), context);

        play(prevSong, songsList, position, artistNameP, imageP, context);
        sendUpdateBroadcast();
    }
}
