package com.example.colink.share;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.colink.Listeners.FileReceiveListener;
import com.example.colink.Listeners.FileSentListener;
import com.example.colink.Loading.ReceiverLoadingActivity;
import com.example.colink.Loading.SenderLoadingActivity;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileClient {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Logger logger = Logger.getLogger(FileClient.class.getName());
    private FileSentListener listener;

    public void uploadFiles(
            Context context, ArrayList<File> files, String senderName, String serverIP, FileSentListener listener) {
        this.listener = listener;
        executorService.execute(new SendTextTask(context, files, senderName, serverIP));
    }

    private class SendTextTask implements Runnable {
        private final Context context;
        private final String senderName;
        private final String serverIP;
        private final ArrayList<File> files;

        public SendTextTask(
                Context context, ArrayList<File> files, String senderName, String serverIP) {
            this.context = context;
            this.senderName = senderName;
            this.serverIP = serverIP;
            this.files = files;
        }

        @Override
        public void run() {
            boolean textSendSuccess;
            try (Socket socket = new Socket(serverIP, 3000);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                dos.writeUTF(senderName);
                dos.flush();

                textSendSuccess = dis.readBoolean();

                if (textSendSuccess && !files.isEmpty()) {
                    dos.writeInt(files.size());
                    dos.flush();
                    startLoadingActivity(files.size());
                    for (File file : files) {
                        uploadFile(context, file, Uri.fromFile(file), file.getName(), file.length(), dos, dis);
                    }
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error sending text message", e);
                textSendSuccess = false;
            }

            final boolean finalResult = textSendSuccess;
            mainHandler.post(
                    () ->
                            Toast.makeText(context, "Sender text result: " + finalResult, Toast.LENGTH_SHORT)
                                    .show());
        }

        private void startLoadingActivity(int size) {
            Intent intent = new Intent(context, SenderLoadingActivity.class);
            intent.putExtra("fileCount", size);
            context.startActivity(intent);
        }

        private void uploadFile(
                Context context,
                File file,
                Uri fileUri,
                String fileName,
                long fileSize,
                DataOutputStream dos,
                DataInputStream dis) {
            boolean fileUploadSuccess;
            try (BufferedInputStream bis =
                         new BufferedInputStream(context.getContentResolver().openInputStream(fileUri))) {

                dos.writeUTF(fileName);
                dos.writeLong(fileSize);

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                dos.flush();

                fileUploadSuccess = dis.readBoolean();
                listener.onFileSent(file);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error uploading file", e);
                fileUploadSuccess = false;
            }

            final boolean finalResult = fileUploadSuccess;
            mainHandler.post(
                    () ->
                            Toast.makeText(context, "Upload result: " + finalResult, Toast.LENGTH_SHORT).show());
        }
    }
}
