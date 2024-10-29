package com.example.colink.Adapters;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.colink.Listeners.ClickListener;
import com.example.colink.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ItemViewHolder> {

    private final HashMap<File, Boolean> songs;
    private final ClickListener listener;
    private Context context;
    private final List<File> itemsList;

    public SongsAdapter(HashMap<File, Boolean> R_songs, ClickListener R_listener) {
        this.songs = R_songs;
        this.listener = R_listener;
        this.itemsList = new ArrayList<>(R_songs.keySet());
    }

    @NonNull
    @Override
    public SongsAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.song, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongsAdapter.ItemViewHolder holder, int position) {
        File file = itemsList.get(position);
        String song_name = file.getName().replace(".mp3", "");
        String artistName = "";
        byte[] image = {};

        holder.songName.setText(song_name);
        try {
            artistName = getArtistName(file.getAbsolutePath());
            holder.artist.setText(artistName != null ? artistName : "Unknown Artist");
        } catch (Exception e) {
            holder.artist.setText("Unknown Artist");
        }

        try {
            image = getAlbumArt(Uri.fromFile(file));
            if (image.length > 0) {
                Glide.with(context).asBitmap().load(image).into(holder.songBanner);
            } else {
                Glide.with(context).load(R.drawable.default_album_art).into(holder.songBanner);
            }
        } catch (Exception e) {
            Glide.with(context).load(R.drawable.default_album_art).into(holder.songBanner);
        }

        byte[] finalImage = image;
        String finalArtistName = artistName;

        if (!songs.get(file)) {
            holder.container.setOnClickListener(v -> {
                listener.onClick(file, position, song_name, finalArtistName, finalImage);
            });
        }
    }

    public static String getArtistName(String filePath) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            retriever.release();
        }
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    private byte[] getAlbumArt(Uri uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            byte[] art = retriever.getEmbeddedPicture();
            return art != null ? art : new byte[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        } finally {
            retriever.release();
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView songBanner;
        TextView songName;
        TextView artist;
        RelativeLayout container;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            songBanner = itemView.findViewById(R.id.songBanner);
            songName = itemView.findViewById(R.id.songName);
            artist = itemView.findViewById(R.id.artist);
            container = itemView.findViewById(R.id.main_song_container);
        }
    }
}
