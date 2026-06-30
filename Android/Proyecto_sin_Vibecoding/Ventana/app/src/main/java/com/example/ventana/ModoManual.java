package com.example.ventana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ModoManual extends AppCompatActivity {

    private TextView tvEstadoVentana;
    private Button btnAbrir, btnCerrar, btnEmergencia;

    // 1. Declarar el receiver
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MqttService.ACTION_STATE_RECEIVED.equals(intent.getAction())) {
                String estado = intent.getStringExtra(MqttService.EXTRA_ESTADO);
                FuncionesGenericas.actualizarEstadoVentana(ModoManual.this, tvEstadoVentana, estado, btnEmergencia);

                // Mover esto AQUÍ ADENTRO (antes de cerrar el primer if):
                if (estado != null) {
                    if (estado.equalsIgnoreCase("BLOQUEADO")) {
                        ConexionESP.getInstancia(ModoManual.this).setVentanaBloqueada(true);
                    } else {
                        ConexionESP.getInstancia(ModoManual.this).setVentanaBloqueada(false);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (MqttService.ACTION_MQTT_MESSAGE.equals(action)) {
                String topic = intent.getStringExtra(MqttService.EXTRA_TOPIC);
                String message = intent.getStringExtra(MqttService.EXTRA_MESSAGE);
                procesarMensaje(topic, message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modo_manual);

        tvEstadoVentana = findViewById(R.id.tv_manual_estado);

        TextView tvSaludo = findViewById(R.id.tv_saludo);
        FuncionesGenericas.configurarSaludoUsuario(this, tvSaludo);

        Button btnExit = findViewById(R.id.btnExit);
        FuncionesGenericas.configurarBotonSalir(this, btnExit);

        Button btnConfigManual = findViewById(R.id.btnConfigManual);
        FuncionesGenericas.configurarBotonConfiguracion(this, btnConfigManual, null);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnVolver = findViewById(R.id.btn_volver);
        btnVolver.setOnClickListener(v -> {
            ConexionESP.getInstancia(ModoManual.this).publicar(ModoManual.this, "/ventana/modo", "AUTOMATICO");
            // Espera breve para asegurar que el mensaje se publique antes de cambiar de actividad
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(ModoManual.this, ModoAutomatico.class);
                startActivity(intent);
                finish();
            }, 150);
        });

        btnAbrir = findViewById(R.id.btn_abrir);
        btnCerrar = findViewById(R.id.btn_cerrar);

        btnAbrir.setOnClickListener(v -> ConexionESP.getInstancia(ModoManual.this).abrir(ModoManual.this));
        btnCerrar.setOnClickListener(v -> ConexionESP.getInstancia(ModoManual.this).cerrar(ModoManual.this));

        btnEmergencia = findViewById(R.id.btn_emergencia);
        btnEmergencia.setOnClickListener(v -> {
            String textoActual = btnEmergencia.getText().toString();
            if (textoActual.equalsIgnoreCase("DESBLOQUEAR")) {
                ConexionESP.getInstancia(ModoManual.this).publicar(ModoManual.this, "/ventana/emergencia", "EMERGENCIA");

            } else {
                ConexionESP.getInstancia(ModoManual.this).enviarEmergencia(ModoManual.this);
            }
        });
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

        IntentFilter filter = new IntentFilter(MqttService.ACTION_MQTT_MESSAGE);
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

    private void procesarMensaje(String topic, String message) {
        if (topic == null) return;
        if (topic.equals("/ventana/estado")) {
            FuncionesGenericas.actualizarEstadoVentana(this, tvEstadoVentana, message, btnEmergencia);
            actualizarBotonesSegunEstado(message);
            if (message.equalsIgnoreCase("BLOQUEADO")) {
                ConexionESP.getInstancia(this).setVentanaBloqueada(true);
            } else {
                ConexionESP.getInstancia(this).setVentanaBloqueada(false);
            }
        }
    }

    private void actualizarBotonesSegunEstado(String estado) {
        if (estado == null || estado.equals("GET")) return;

        if (estado.equalsIgnoreCase("ABIERTA") || estado.equalsIgnoreCase("ABIERTO")) {
            btnAbrir.setEnabled(false);
            btnAbrir.setAlpha(0.5f);
            btnCerrar.setEnabled(true);
            btnCerrar.setAlpha(1.0f);
        } else if (estado.equalsIgnoreCase("CERRADA") || estado.equalsIgnoreCase("CERRADO")) {
            btnCerrar.setEnabled(false);
            btnCerrar.setAlpha(0.5f);
            btnAbrir.setEnabled(true);
            btnAbrir.setAlpha(1.0f);
        } else {
            // Si está en movimiento (ABRIENDO/CERRANDO), habilitar ambos o según lógica deseada
            // Aquí habilitamos ambos para permitir detener o invertir si el ESP lo soporta
            btnAbrir.setEnabled(true);
            btnAbrir.setAlpha(1.0f);
            btnCerrar.setEnabled(true);
            btnCerrar.setAlpha(1.0f);
        }
    }
}
