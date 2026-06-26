package com.example.ventana;

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

public class ModoAutomatico extends AppCompatActivity {

    private TextView tvMqttStatus, tvTemp, tvHumedad, tvEstadoVentana, tvModoActual;
    
    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MqttService.ACTION_MQTT_STATUS.equals(action)) {
                boolean conectado = intent.getBooleanExtra(MqttService.EXTRA_CONNECTED, false);
                actualizarEstadoConexion(conectado);
            } else if (MqttService.ACTION_MQTT_MESSAGE.equals(action)) {
                String topic = intent.getStringExtra(MqttService.EXTRA_TOPIC);
                String message = intent.getStringExtra(MqttService.EXTRA_MESSAGE);
                procesarMensaje(topic, message);
            }
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
        tvModoActual = findViewById(R.id.tv_modo_actual);

        TextView tvSaludo = findViewById(R.id.tv_saludo);
        FuncionesGenericas.configurarSaludoUsuario(this, tvSaludo);

        Button btnExit2 = findViewById(R.id.btnExit2);
        FuncionesGenericas.configurarBotonSalir(this, btnExit2);

        Button btnConfig = findViewById(R.id.btnConfig);
        FuncionesGenericas.configurarBotonConfiguracion(this, btnConfig, this::iniciarServicioMqtt);

        Button btn_manual = findViewById(R.id.btn_modo_manual);
        btn_manual.setOnClickListener(v -> {
            ConexionESP.getInstancia(ModoAutomatico.this).publicar(ModoAutomatico.this, "/ventana/modo", "MANUAL");
            Intent intent = new Intent(ModoAutomatico.this, ModoManual.class);
            startActivity(intent);
            finish();
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
        IntentFilter filter = new IntentFilter();
        filter.addAction(MqttService.ACTION_MQTT_STATUS);
        filter.addAction(MqttService.ACTION_MQTT_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttReceiver, filter);
        
        ConexionESP.getInstancia(this).iniciarDeteccionShake(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttReceiver);
        ConexionESP.getInstancia(this).detenerDeteccionShake();
    }

    private void actualizarEstadoConexion(boolean conectado) {
        if (conectado) {
            tvMqttStatus.setText("Conectado");
            tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.estado_conectado));
        } else {
            tvMqttStatus.setText("Desconectado");
            tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.estado_desconectado));
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
                FuncionesGenericas.actualizarEstadoVentana(this, tvEstadoVentana, message);
                break;
            case "/ventana/modo":
                FuncionesGenericas.actualizarModoActual(this, tvModoActual, message);
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
