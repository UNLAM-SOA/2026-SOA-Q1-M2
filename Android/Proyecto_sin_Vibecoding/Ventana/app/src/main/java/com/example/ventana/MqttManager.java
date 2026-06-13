package com.example.ventana;

import android.util.Log;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttManager {
    private static final String TAG = "MqttManager";
    private static MqttManager instance;
    private MqttAsyncClient mqttClient;
    private MqttCallbackListener callbackListener;

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
        this.callbackListener = listener;
    }

    public void connect(String brokerUrl, String clientId, String username, String password) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }

            mqttClient = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            if (!username.isEmpty()) options.setUserName(username);
            if (!password.isEmpty()) options.setPassword(password.toCharArray());

            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "Conectado a: " + serverURI);
                    if (callbackListener != null) callbackListener.onConnectionStatusChanged(true);
                    subscribeToTopics();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Conexión perdida");
                    if (callbackListener != null) callbackListener.onConnectionStatusChanged(false);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    if (callbackListener != null) callbackListener.onMessageReceived(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttClient.connect(options);
        } catch (MqttException e) {
            Log.e(TAG, "Error MQTT: " + e.getMessage());
        }
    }

    private void subscribeToTopics() {
        try {
            // Tópicos idénticos a las constantes del ESP32
            String[] topics = {"/ventana/humedad", "/ventana/temp", "/ventana/estado", "/ventana/modo"};
            int[] qos = {1, 1, 1, 1};
            mqttClient.subscribe(topics, qos);
        } catch (MqttException e) {
            Log.e(TAG, "Error suscribiendo: " + e.getMessage());
        }
    }

    public void publish(String topic, String payload) {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);
                mqttClient.publish(topic, message);
                Log.d(TAG, "Publicado en " + topic + ": " + payload);
            } catch (MqttException e) {
                Log.e(TAG, "Error publicando: " + e.getMessage());
            }
        }
    }
}
