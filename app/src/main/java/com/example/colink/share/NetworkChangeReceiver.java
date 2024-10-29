package com.example.colink.share;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private NetworkChangeListener networkChangeListener;

    // Default constructor
    public NetworkChangeReceiver() {

    }

    // Constructor with listener
    public NetworkChangeReceiver(NetworkChangeListener listener) {
        this.networkChangeListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (networkChangeListener != null) {
            networkChangeListener.onNetworkChanged(isConnected);
        }
    }

    public void setNetworkChangeListener(NetworkChangeListener listener) {
        this.networkChangeListener = listener;
    }

    public interface NetworkChangeListener {
        void onNetworkChanged(boolean isConnected);
    }
}
