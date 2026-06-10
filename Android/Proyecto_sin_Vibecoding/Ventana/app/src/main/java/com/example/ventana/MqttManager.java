package com.example.ventana;

import android.util.Log;

public class MqttManager {
    private static final String TAG = "MqttManager";
    private static MqttManager instance;
    private MqttCallbackListener listener;

    public interface MqttCallbackListener {
        void onConnectionStatusChanged(boolean isConnected);
        void onMessageReceived(String topic, String message);
    }

    private MqttManager() {}

    public static synchronized MqttManager getInstance() {
        if (instance == null) {
            instance = new MqttManager();
        }
        return instance;
    }

    public void setCallbackListener(MqttCallbackListener listener) {
        this.listener = listener;
    }

    public void connect(String brokerUrl, String clientId, String username, String password) {
        Log.d(TAG, "Conectando a " + brokerUrl);
        // Aquí iría la lógica de conexión con una librería como Paho
        // Simulamos conexión exitosa para propósitos de demostración
        if (listener != null) {
            listener.onConnectionStatusChanged(true);
        }
    }

    public void publish(String topic, String message) {
        Log.d(TAG, "Publicando en " + topic + ": " + message);
        // Aquí iría la lógica de publicación
    }

    // Método para simular la recepción de mensajes (para probar la UI)
    public void simulateMessage(String topic, String message) {
        if (listener != null) {
            listener.onMessageReceived(topic, message);
        }
    }
}
