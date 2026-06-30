package com.example.ventana;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
@SuppressLint("SetTextI18n") //clase que suprime el warning que pide que no hacodees para que la app pueda ser traducida en el futuro
public class ModoAutomatico extends AppCompatActivity {

    private TextView tvMqttStatus, tvTemp, tvHumedad, tvEstadoVentana;
    private Button btnEmergencia;
    // 1. Declarar el receiver
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MqttService.ACTION_STATE_RECEIVED.equals(intent.getAction())) {
                String estado = intent.getStringExtra(MqttService.EXTRA_ESTADO);

                FuncionesGenericas.actualizarEstadoVentana(ModoAutomatico.this, tvEstadoVentana, estado, btnEmergencia);
                if (estado != null) {
                    if (estado.equalsIgnoreCase("BLOQUEADO")) {
                        ConexionESP.getInstancia(ModoAutomatico.this).setVentanaBloqueada(true);
                    } else {
                        ConexionESP.getInstancia(ModoAutomatico.this).setVentanaBloqueada(false);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String topic = intent.getStringExtra(MqttService.EXTRA_TOPIC);
            String message = intent.getStringExtra(MqttService.EXTRA_MESSAGE);
            if (MqttService.ACTION_MQTT_STATUS.equals(action)) {
                boolean conectado = intent.getBooleanExtra(MqttService.EXTRA_CONNECTED, false);
                actualizarEstadoConexion(conectado);
            }
            procesarMensaje(topic, message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automatico);

        tvMqttStatus = findViewById(R.id.tv_mqtt_status);
        tvTemp = findViewById(R.id.tv_temp);
        tvHumedad = findViewById(R.id.tv_humedad);
        tvEstadoVentana = findViewById(R.id.tv_estado_ventana);

        TextView tvSaludo = findViewById(R.id.tv_saludo);
        FuncionesGenericas.configurarSaludoUsuario(this, tvSaludo);

        Button btnExit2 = findViewById(R.id.btnExit2);
        FuncionesGenericas.configurarBotonSalir(this, btnExit2);

        Button btnConfig = findViewById(R.id.btnConfig);
        FuncionesGenericas.configurarBotonConfiguracion(this, btnConfig, this::iniciarServicioMqtt);

        Button btn_manual = findViewById(R.id.btn_modo_manual);
        btn_manual.setOnClickListener(v -> {
            ConexionESP.getInstancia(ModoAutomatico.this).publicar(ModoAutomatico.this, "/ventana/modo", "MANUAL");
            // Espera breve para asegurar que el mensaje se publique antes de cambiar de actividad
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(ModoAutomatico.this, ModoManual.class);
                startActivity(intent);
                finish();
            }, 150);
        });

        btnEmergencia = findViewById(R.id.btn_emergencia);
        btnEmergencia.setOnClickListener(v -> {
            String textoActual = btnEmergencia.getText().toString();
            if (textoActual.equalsIgnoreCase("DESBLOQUEAR")) {
                ConexionESP.getInstancia(ModoAutomatico.this).publicar(ModoAutomatico.this, "/ventana/emergencia", "EMERGENCIA");

            } else {
                ConexionESP.getInstancia(ModoAutomatico.this).enviarEmergencia(ModoAutomatico.this);
            }
        });

        iniciarServicioMqtt();
    }

    private void iniciarServicioMqtt() {
        ConexionESP conexion = ConexionESP.getInstancia(this);
        conexion.cargarConfiguracion(this);
        conexion.conectar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 2. Registrar el receiver para escuchar las respuestas del servicio
        LocalBroadcastManager.getInstance(this).registerReceiver(
            stateReceiver, 
            new IntentFilter(MqttService.ACTION_STATE_RECEIVED)
        );
        
        // 3. Enviar un intent al MqttService solicitando que se resuscriba.
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            new Intent(MqttService.ACTION_REQUEST_STATE)
        );

        IntentFilter filter = new IntentFilter();
        filter.addAction(MqttService.ACTION_MQTT_STATUS);
        filter.addAction(MqttService.ACTION_MQTT_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttReceiver, filter);
        
        ConexionESP.getInstancia(this).iniciarDeteccionShake(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttReceiver);
        ConexionESP.getInstancia(this).detenerDeteccionShake();
    }

    private void actualizarEstadoConexion(boolean conectado) {
        if (conectado) {
            tvMqttStatus.setText("Conectado");
            tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.estado_conectado));
        } else {
            tvMqttStatus.setText("Desconectado");
            tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.color_rojo));
        }
    }

    private void procesarMensaje(String topic, String message) {
        if (topic == null) return;
        switch (topic) {
            case "/ventana/temp":
                tvTemp.setText("Temp: " + message + " °C");
                break;
            case "/ventana/humedad":
                tvHumedad.setText("Humedad: " + message + " %");
                break;
            case "/ventana/estado":
                FuncionesGenericas.actualizarEstadoVentana(this, tvEstadoVentana, message, btnEmergencia);
                if (message.equalsIgnoreCase("BLOQUEADO")) {
                    ConexionESP.getInstancia(this).setVentanaBloqueada(true);
                } else {
                    ConexionESP.getInstancia(this).setVentanaBloqueada(false);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Configuración");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            ConexionESP.getInstancia(this).mostrarDialogoConfiguracion(this, this::iniciarServicioMqtt);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
