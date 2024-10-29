package com.example.colink.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.colink.Listeners.FileReceiveListener;
import com.example.colink.ReceiveActivity;
import com.example.colink.share.FileServer;

import java.io.File;

public class FileServerService extends Service implements FileReceiveListener {
    private FileServer fileServer;
    private final IBinder binder = new LocalBinder();
    private GetReceiveFile getReceiveFile;

    public class LocalBinder extends Binder {
        public FileServerService getService() {
            return FileServerService.this;
        }
    }

    public interface GetReceiveFile {
        void updateProgress(File file);
    }

    public void setGetReceiveFileListener(GetReceiveFile listener) {
        this.getReceiveFile = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void start(String IP, Context context) {
        fileServer = new FileServer(IP, 3000, context, this);
        fileServer.start();
    }

    public void stop() {
        if (fileServer != null) {
            fileServer.stop();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fileServer != null) {
            fileServer.stop();
        }
    }

    @Override
    public void onFileReceived(File receivedFile) {
        if (getReceiveFile != null) {
            getReceiveFile.updateProgress(receivedFile);
        }
    }
}
