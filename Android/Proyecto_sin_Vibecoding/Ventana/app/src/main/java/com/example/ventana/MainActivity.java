package com.example.ventana;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.view.View;

public class MainActivity extends AppCompatActivity implements MqttManager.MqttCallbackListener {

    private TextView tvMqttStatus, tvTemp, tvHumedad, tvEstadoVentana, tvModoActual;
    private String modoActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automatico);

        // Vincular vistas
        tvMqttStatus = findViewById(R.id.tv_mqtt_status);
        tvTemp = findViewById(R.id.tv_temp);
        tvHumedad = findViewById(R.id.tv_humedad);
        tvEstadoVentana = findViewById(R.id.tv_estado_ventana);
        tvModoActual = findViewById(R.id.tv_modo_actual);

        Button btn_manual = findViewById(R.id.btn_modo_manual);

        // Acción del botón Modo Manual (Publica, salta de pantalla y cierra la actual)
        btn_manual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Notificar al broker MQTT
                MqttManager.getInstance().publish("/ventana/modo", "MANUAL");

                // 2. Saltar a la pantalla de modoManual
                Intent intent = new Intent(MainActivity.this, modoManual.class);
                startActivity(intent);

                // 3. Destruir MainActivity para que no quede de fondo
                finish();
            }
        });


        conectarMqtt();
    }

    private void conectarMqtt() {
        SharedPreferences prefs = getSharedPreferences("ConfigMQTT", MODE_PRIVATE);
        String host = prefs.getString("host", "tcp://broker.emqx.io");
        String port = prefs.getString("port", "1883");
        String user = prefs.getString("user", "");
        String pass = prefs.getString("pass", "");

        MqttManager.getInstance().setCallbackListener(this);
        MqttManager.getInstance().connect(host + ":" + port, "AndroidClient_" + System.currentTimeMillis(), user, pass);
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                tvMqttStatus.setText("Conectado");
                tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.estado_conectado));
            } else {
                tvMqttStatus.setText("Desconectado");
                tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.estado_desconectado));
            }
        });
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        runOnUiThread(() -> {
            switch (topic) {
                case "/ventana/temp":
                    tvTemp.setText("Temp: " + message + " °C");
                    break;
                case "/ventana/humedad":
                    tvHumedad.setText("Humedad: " + message + " %");
                    break;
                case "/ventana/estado":
                    tvEstadoVentana.setText(message);
                    if (message.equals("ABRIENDO") || message.equals("CERRANDO")) {
                        tvEstadoVentana.setTextColor(ContextCompat.getColor(this, R.color.estado_movimiento));
                    } else {
                        tvEstadoVentana.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                    }
                    break;
                case "/ventana/modo":
                    modoActual = message;
                    tvModoActual.setText(message);
                    if (message.equals("AUTOMATICO")) {
                        tvModoActual.setTextColor(ContextCompat.getColor(this, R.color.estado_automatico));
                    } else {
                        tvModoActual.setTextColor(ContextCompat.getColor(this, R.color.estado_manual));
                    }
                    break;
            }
        });
    }




}