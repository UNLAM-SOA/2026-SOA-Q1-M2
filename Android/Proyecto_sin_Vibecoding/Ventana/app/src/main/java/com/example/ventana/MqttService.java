package com.example.ventana;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttService extends Service {
    private static final String TAG = "MqttService";
    private static final String CHANNEL_ID = "MqttServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    // Constantes para Broadcast
    public static final String ACTION_MQTT_MESSAGE = "com.example.ventana.MQTT_MESSAGE";
    public static final String EXTRA_TOPIC = "topic";
    public static final String EXTRA_MESSAGE = "message";
    
    public static final String ACTION_MQTT_STATUS = "com.example.ventana.MQTT_STATUS";
    public static final String EXTRA_CONNECTED = "connected";

    // Acciones para el Servicio
    public static final String ACTION_CONNECT = "com.example.ventana.CONNECT";
    public static final String ACTION_PUBLISH = "com.example.ventana.PUBLISH";

    private MqttAsyncClient mqttClient;
    private ConectividadManager conectividadManager;
    private String lastHost, lastPort, lastUser, lastPass;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio MqttService creado");
        createNotificationChannel();
        
        // Inicializar monitoreo de red
        conectividadManager = new ConectividadManager(this, new ConectividadManager.NetworkStatusListener() {
            @Override
            public void onNetworkAvailable() {
                Log.d(TAG, "Red disponible, intentando reconectar MQTT si es necesario...");
                if (lastHost != null && (mqttClient == null || !mqttClient.isConnected())) {
                    conectarMqtt(lastHost, lastPort, lastUser, lastPass);
                }
            }

            @Override
            public void onNetworkLost() {
                Log.d(TAG, "Red perdida, MQTT se desconectará pronto");
                notifyStatus(false);
            }
        });
        conectividadManager.startMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_CONNECT.equals(action)) {
                lastHost = intent.getStringExtra("host");
                lastPort = intent.getStringExtra("port");
                lastUser = intent.getStringExtra("user");
                lastPass = intent.getStringExtra("pass");
                
                startForeground(NOTIFICATION_ID, getNotification("Conectando a MQTT..."));
                conectarMqtt(lastHost, lastPort, lastUser, lastPass);
            } else if (ACTION_PUBLISH.equals(action)) {
                String topic = intent.getStringExtra("topic");
                String message = intent.getStringExtra("message");
                publish(topic, message);
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Servicio de Monitoreo de Ventana",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(String contentText) {
        Intent notificationIntent = new Intent(this, ModoAutomatico.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Servicio Ventana Activo")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String contentText) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(contentText));
        }
    }

    private synchronized void conectarMqtt(String host, String port, String user, String pass) {
        if (!conectividadManager.isConnected()) {
            Log.w(TAG, "No hay internet. No se puede conectar a MQTT");
            updateNotification("Esperando conexión a internet...");
            notifyStatus(false);
            return;
        }

        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }

            String brokerUrl = host + ":" + port;
            mqttClient = new MqttAsyncClient(brokerUrl, "AndroidClient_" + System.currentTimeMillis(), new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);
            if (user != null && !user.isEmpty()) options.setUserName(user);
            if (pass != null && !pass.isEmpty()) options.setPassword(pass.toCharArray());

            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "MQTT Conectado: " + serverURI);
                    updateNotification("Conectado al servidor de ventana");
                    notifyStatus(true);
                    subscribeToTopics();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "MQTT Conexión perdida");
                    updateNotification("Desconectado (Reintentando...)");
                    notifyStatus(false);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    notifyMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttClient.connect(options);
        } catch (MqttException e) {
            Log.e(TAG, "Error MQTT al conectar: " + e.getMessage());
            updateNotification("Error de conexión MQTT");
            notifyStatus(false);
        }
    }

    private void subscribeToTopics() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                String[] topics = {"/ventana/humedad", "/ventana/temp", "/ventana/estado", "/ventana/modo"};
                int[] qos = {1, 1, 1, 1};
                mqttClient.subscribe(topics, qos);
                Log.d(TAG, "Suscrito a los tópicos de la ventana");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error suscribiendo: " + e.getMessage());
        }
    }

    private void publish(String topic, String payload) {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);
                mqttClient.publish(topic, message);
                Log.d(TAG, "Publicado en " + topic + ": " + payload);
            } catch (MqttException e) {
                Log.e(TAG, "Error publicando: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "No se puede publicar, MQTT no está conectado");
        }
    }

    private void notifyStatus(boolean connected) {
        Intent intent = new Intent(ACTION_MQTT_STATUS);
        intent.putExtra(EXTRA_CONNECTED, connected);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void notifyMessage(String topic, String message) {
        Intent intent = new Intent(ACTION_MQTT_MESSAGE);
        intent.putExtra(EXTRA_TOPIC, topic);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Servicio MqttService destruyéndose");
        conectividadManager.stopMonitoring();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error al desconectar MQTT", e);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
