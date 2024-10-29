package com.example.colink.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.colink.Listeners.FileSentListener;
import com.example.colink.share.FileClient;

import java.io.File;
import java.util.ArrayList;

public class FileClientService extends Service implements FileSentListener {
    private FileClient fileClient;
    private final IBinder binder = new LocalBinder();
    private GetSentFile getSentFile;

    public class LocalBinder extends Binder {
        public FileClientService getService() {
            return FileClientService.this;
        }
    }

    public interface GetSentFile {
        void updateProgress(File file);
    }

    public void setGetSentFileListener(GetSentFile listener) {
        this.getSentFile = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void upload(String IP, Context context, ArrayList<File> selectedItems, String name) {
        fileClient = new FileClient();
        fileClient.uploadFiles(context, selectedItems, name, IP, this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onFileSent(File sentFile) {
        if (getSentFile != null) {
            getSentFile.updateProgress(sentFile);
        }
    }
}
