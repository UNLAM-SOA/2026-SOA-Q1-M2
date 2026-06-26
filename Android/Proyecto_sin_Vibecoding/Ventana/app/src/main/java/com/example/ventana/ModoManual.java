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

    private TextView tvEstadoVentana, tvModoActual;

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
        tvModoActual = findViewById(R.id.tv_modo_actual);

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
            Intent intent = new Intent(ModoManual.this, ModoAutomatico.class);
            startActivity(intent);
            finish();
        });

        Button btnAbrir = findViewById(R.id.btn_abrir);
        Button btnCerrar = findViewById(R.id.btn_cerrar);

        btnAbrir.setOnClickListener(v -> ConexionESP.getInstancia(ModoManual.this).abrir(ModoManual.this));
        btnCerrar.setOnClickListener(v -> ConexionESP.getInstancia(ModoManual.this).cerrar(ModoManual.this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MqttService.ACTION_MQTT_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttReceiver, filter);
        
        ConexionESP.getInstancia(this).iniciarDeteccionShake(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttReceiver);
        ConexionESP.getInstancia(this).detenerDeteccionShake();
    }

    private void procesarMensaje(String topic, String message) {
        if (topic == null) return;
        if (topic.equals("/ventana/estado")) {
            FuncionesGenericas.actualizarEstadoVentana(this, tvEstadoVentana, message);
        } else if (topic.equals("/ventana/modo")) {
            FuncionesGenericas.actualizarModoActual(this, tvModoActual, message);
        }
    }
}
