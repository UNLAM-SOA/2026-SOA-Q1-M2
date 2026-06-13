package com.example.ventana;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.view.View;

public class ModoAutomatico extends AppCompatActivity implements ConexionESP.CallbackConexion {

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

        // Usar clase FuncionesGenericas para configurar saludo, cerrar sesión y diálogo de configuración
        TextView tvSaludo = findViewById(R.id.tv_saludo);
        FuncionesGenericas.configurarSaludoUsuario(this, tvSaludo);

        Button btnExit2 = findViewById(R.id.btnExit2);
        FuncionesGenericas.configurarBotonSalir(this, btnExit2);

        Button btnConfig = findViewById(R.id.btnConfig);
        FuncionesGenericas.configurarBotonConfiguracion(this, btnConfig, () -> conectarMqtt());

        Button btn_manual = findViewById(R.id.btn_modo_manual);

        // Acción del botón Modo Manual (Publica, salta de pantalla y cierra la actual)
        btn_manual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Notificar al broker MQTT usando ConexionESP
                ConexionESP.getInstancia(ModoAutomatico.this).publicar("/ventana/modo", "MANUAL");

                // 2. Saltar a la pantalla de modoManual
                Intent intent = new Intent(ModoAutomatico.this, ModoManual.class);
                startActivity(intent);

                // 3. Destruir MainActivity para que no quede de fondo
                finish();
            }
        });

        conectarMqtt();
    }

    private void conectarMqtt() {
        // Inicializar/actualizar la clase ConexionESP y registrar esta actividad como callback
        ConexionESP conexion = ConexionESP.getInstancia(this);
        conexion.setCallback(this);
        conexion.cargarConfiguracion(this);
        conexion.conectar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Iniciar la detección de Shake al volver a la pantalla
        ConexionESP.getInstancia(this).iniciarDeteccionShake(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detener la detección de Shake para evitar uso innecesario de sensores
        ConexionESP.getInstancia(this).detenerDeteccionShake();
    }

    @Override
    public void onEstadoConexionCambiado(boolean conectado) {
        runOnUiThread(() -> {
            if (conectado) {
                tvMqttStatus.setText("Conectado");
                tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.estado_conectado));
            } else {
                tvMqttStatus.setText("Desconectado");
                tvMqttStatus.setTextColor(ContextCompat.getColor(this, R.color.estado_desconectado));
            }
        });
    }

    @Override
    public void onMensajeRecibido(String topic, String message) {
        runOnUiThread(() -> {
            switch (topic) {
                case "/ventana/temp":
                    tvTemp.setText("Temp: " + message + " °C");
                    break;
                case "/ventana/humedad":
                    tvHumedad.setText("Humedad: " + message + " %");
                    break;
                case "/ventana/estado":
                    // Abstraer la lógica del estado de la ventana en FuncionesGenericas
                    FuncionesGenericas.actualizarEstadoVentana(this, tvEstadoVentana, message);
                    break;
                case "/ventana/modo":
                    modoActual = message;
                    // Abstraer la lógica del modo en FuncionesGenericas
                    FuncionesGenericas.actualizarModoActual(this, tvModoActual, message);
                    break;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Configuración");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            ConexionESP.getInstancia(this).mostrarDialogoConfiguracion(this, () -> conectarMqtt());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}