package com.example.ventana;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

public class ConectividadManager {
    private static final String TAG = "ConectividadManager";

    public interface NetworkStatusListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private final ConnectivityManager connectivityManager;
    private final NetworkStatusListener listener;
    private ConnectivityManager.NetworkCallback networkCallback;

    public ConectividadManager(Context context, NetworkStatusListener listener) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
    }

    public void startMonitoring() {
        if (networkCallback != null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Red disponible");
                if (listener != null) listener.onNetworkAvailable();
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Red perdida");
                if (listener != null) listener.onNetworkLost();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                Log.d(TAG, "Capacidades cambiadas. Tiene internet: " + hasInternet);
                if (hasInternet) {
                    if (listener != null) listener.onNetworkAvailable();
                } else {
                    if (listener != null) listener.onNetworkLost();
                }
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void stopMonitoring() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    public boolean isConnected() {
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
