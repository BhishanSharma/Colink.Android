package com.example.colink.share;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.colink.Listeners.FileReceiveListener;
import com.example.colink.Loading.ReceiverLoadingActivity;
import com.example.colink.R;
import com.example.colink.ReceiveActivity;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {
    private static int PORT;
    private static String IP;
    private ServerSocket serverSocket;
    private boolean textMessageResponse = false;
    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final FileReceiveListener listener;

    public FileServer(String R_IP, int R_PORT, Context R_context, FileReceiveListener listener) {
        IP = R_IP;
        PORT = R_PORT;
        context = R_context;
        mainHandler = new Handler(Looper.getMainLooper());
        this.listener = listener;
    }

    public void start() {
        executorService.execute(new ServerRunnable());
    }

    private class ServerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName(IP));
                showToast("Server started at " + IP + ":" + PORT + ", waiting for client...");

                int connectionsCount = 1;
                while (connectionsCount <= 10) {
                    try (Socket clientSocket = serverSocket.accept()) {
                        showToast("Client connected");
                        handleClientSocket(clientSocket);
                    }
                    connectionsCount++;
                }

            } catch (IOException e) {
                showToast("Server error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                stop();
            }
        }

        private void handleClientSocket(Socket clientSocket) {
            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                String textMessage = dis.readUTF();
                showToast("Received text message: " + textMessage);
                boolean success = processTextMessage(textMessage);

                dos.writeBoolean(success);
                dos.flush();

                if (success) {
                    int fileCount = dis.readInt();
                    startLoadingActivity(fileCount);
                    for (int i = 0; i < fileCount; i++) {
                        receiveFile(dis, dos);
                    }
                    stop();
                }

            } catch (IOException e) {
                showToast("Error reading from client: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private boolean processTextMessage(String textMessage) {
            showConfirmationDialog(textMessage);
            return textMessageResponse;
        }

        private void showConfirmationDialog(String textMessage) {
            mainHandler.post(
                    () -> {
                        Dialog dialog = new Dialog(context);
                        dialog.setContentView(R.layout.custom_dialog_box);
                        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        dialog.getWindow().setBackgroundDrawable(context.getDrawable(R.drawable.custom_dialog_box_back));
                        dialog.setCancelable(false);

                        TextView textViewMessage = dialog.findViewById(R.id.textView2);
                        textViewMessage.setText("Do you want to accept the file from: " + textMessage + "?");

                        Button btnDialogAccept = dialog.findViewById(R.id.btnDialogAccept);
                        Button btnDialogCancel = dialog.findViewById(R.id.btnDialogCancel);

                        btnDialogAccept.setOnClickListener(v -> {
                            synchronized (FileServer.this) {
                                textMessageResponse = true;
                                FileServer.this.notifyAll();
                            }
                            dialog.dismiss();
                        });

                        btnDialogCancel.setOnClickListener(v -> {
                            synchronized (FileServer.this) {
                                textMessageResponse = false;
                                FileServer.this.notifyAll();
                            }
                            dialog.dismiss();
                        });

                        dialog.show();
                    });

            synchronized (FileServer.this) {
                try {
                    FileServer.this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void startLoadingActivity(int fileCount) {
            Intent intent = new Intent(context, ReceiverLoadingActivity.class);
            intent.putExtra("fileCount", fileCount);
            context.startActivity(intent);
        }

        private void receiveFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();
            long fileLength = dis.readLong();

            File mainFolder = new File(Environment.getExternalStorageDirectory(), "CoLink");

            // Ensure main folder and subfolders exist
            File imagesFolder = new File(mainFolder, "Images");
            File songsFolder = new File(mainFolder, "Songs");
            File documentsFolder = new File(mainFolder, "Documents");
            File appsFolder = new File(mainFolder, "Apps");

            if (!mainFolder.exists()) {
                mainFolder.mkdirs();
            }
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs();
            }
            if (!songsFolder.exists()) {
                songsFolder.mkdirs();
            }
            if (!documentsFolder.exists()) {
                documentsFolder.mkdirs();
            }
            if (!appsFolder.exists()) {
                appsFolder.mkdirs();
            }

            File receivedFile = new File(imagesFolder, fileName);
            if (fileName.endsWith(".mp3")) {
                receivedFile = new File(songsFolder, fileName);
            } else if (fileName.endsWith(".pdf")) {
                receivedFile = new File(documentsFolder, fileName);
            } else if (fileName.endsWith(".apk")) {
                receivedFile = new File(appsFolder, fileName);
            }

            try (FileOutputStream fos = new FileOutputStream(receivedFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileLength && (bytesRead = dis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if (fileLength > 0 && totalBytesRead % (fileLength / 10) == 0) {
                        int progress = (int) ((totalBytesRead * 100) / fileLength);
                        updateProgress(progress);
                    }
                }

                bos.flush();
                showToast("File received successfully: " + receivedFile.getAbsolutePath());
                listener.onFileReceived(receivedFile);
                dos.writeBoolean(true);
                dos.flush();

            } catch (IOException e) {
                showToast("Error handling client connection: " + e.getMessage());
                e.printStackTrace();
                dos.writeBoolean(false);
                dos.flush();
            }
        }

        private void updateProgress(int progress) {
            mainHandler.post(
                    () -> Toast.makeText(context, "Progress: " + progress + "%", Toast.LENGTH_SHORT).show());
        }
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            showToast("Error closing server socket: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
