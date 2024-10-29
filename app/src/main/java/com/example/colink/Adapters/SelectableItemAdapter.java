package com.example.colink.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.colink.Listeners.SelectListener;
import com.example.colink.Listeners.openListener;
import com.example.colink.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SelectableItemAdapter extends RecyclerView.Adapter<SelectableItemAdapter.ItemViewHolder> {

    private List<File> itemsList;
    private HashMap<File, Boolean> itemsMap;
    private final SelectListener listener;
    private final Context context;
    private final openListener openListener;

    public SelectableItemAdapter(@NonNull HashMap<File, Boolean> items, Context context, SelectListener listener, openListener openListener) {
        this.itemsMap = items;
        this.itemsList = new ArrayList<>(items.keySet());
        this.listener = listener;
        this.context = context;
        this.openListener = openListener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
        return new ItemViewHolder(view);
    }

    public void updateData(HashMap<File, Boolean> newFileList) {
        this.itemsMap = newFileList;
        this.itemsList = new ArrayList<>(newFileList.keySet());
        notifyDataSetChanged();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        File file = itemsList.get(position);
        holder.itemName.setText(file.getName().replace(".mp3", ""));
        holder.size.setText(String.format("%.2f MB", convertBytesToMB(file.length())));

        String fileName = file.getName();
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            Glide.with(context).load(file).into(holder.imageView);
        } else if (fileName.endsWith(".mp3")) {
            try {
                byte[] image = getAlbumArt(Uri.fromFile(file));
                if (image.length > 0) {
                    Glide.with(context).asBitmap().load(image).into(holder.imageView);
                } else {
                    Glide.with(context).load(R.drawable.default_album_art).into(holder.imageView);
                }
            } catch (IOException e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                Glide.with(context).load(R.drawable.default_album_art).into(holder.imageView);
            }
        } else if (fileName.endsWith(".pdf")) {
            Glide.with(context).load(R.drawable.pdf).into(holder.imageView);
        } else if (fileName.endsWith(".apk")) {
            holder.itemName.setText(getAppName(file.getAbsolutePath()));
            holder.imageView.setImageDrawable(getAppIcon(file.getAbsolutePath()));
        }

        if (listener != null) {
            // Set onClickListener for item selection
            holder.container.setOnClickListener(v -> {
                boolean currentState = Boolean.TRUE.equals(itemsMap.get(file));
                itemsMap.put(file, !currentState);
                holder.check_mark.setVisibility(!currentState ? View.VISIBLE : View.GONE);
                if (!currentState) {
                    listener.onItemSelect(file);
                } else {
                    listener.onItemDeSelect(file);
                }
            });
            // Manage visibility of check mark based on item state
            holder.check_mark.setVisibility(Boolean.TRUE.equals(itemsMap.get(file)) ? View.VISIBLE : View.GONE);
        }

        if (openListener != null) {
            holder.container.setOnClickListener(v -> openListener.open(file));
        }
    }

    private Drawable getAppIcon(String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(apkPath, 0);
        if (pi != null) {
            ApplicationInfo appInfo = pi.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            return appInfo.loadIcon(pm);
        }
        return null;
    }

    private CharSequence getAppName(String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(apkPath, 0);
        if (pi != null) {
            ApplicationInfo appInfo = pi.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            return appInfo.loadLabel(pm);
        }
        return null;
    }

    private byte[] getAlbumArt(Uri uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art != null ? art : new byte[0];
    }

    private double convertBytesToMB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView itemName;
        TextView size;
        RelativeLayout container;
        ImageView check_mark;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            itemName = itemView.findViewById(R.id.itemName);
            size = itemView.findViewById(R.id.itemSize);
            container = itemView.findViewById(R.id.main_item_container);
            check_mark = itemView.findViewById(R.id.selectedIcon);
        }
    }
}
