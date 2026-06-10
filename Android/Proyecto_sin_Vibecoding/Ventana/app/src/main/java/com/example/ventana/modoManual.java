package com.example.ventana;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ModoManual extends AppCompatActivity implements conexion_ESP.CallbackConexion {

    private TextView tvEstadoVentana, tvModoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modo_manual);

        // Vincular vistas de estado
        tvEstadoVentana = findViewById(R.id.tv_manual_estado);
        tvModoActual = findViewById(R.id.tv_modo_actual);

        // Usar clase FuncionesGenericas para configurar saludo, cerrar sesión y diálogo de configuración
        TextView tvSaludo = findViewById(R.id.tv_saludo);
        FuncionesGenericas.configurarSaludoUsuario(this, tvSaludo);

        Button btnExit = findViewById(R.id.btnExit);
        FuncionesGenericas.configurarBotonSalir(this, btnExit);

        Button btnConfigManual = findViewById(R.id.btnConfigManual);
        FuncionesGenericas.configurarBotonConfiguracion(this, btnConfigManual, null);

        // Configuración de los márgenes del sistema (EdgeToEdge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Vinculamos el botón de volver de tu XML
        Button btnVolver = findViewById(R.id.btn_volver);

        // 2. Programamos el clic para regresar a Automático (MainActivity)
        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Notificamos al broker MQTT el cambio de modo usando conexion_ESP
                conexion_ESP.getInstancia(ModoManual.this).publicar("/ventana/modo", "AUTOMATICO");

                // Creamos el salto de regreso a MainActivity
                Intent intent = new Intent(ModoManual.this, ModoAutomatico.class);
                startActivity(intent);

                // Destruimos modoManual para que no quede de fondo
                finish();
            }
        });

        // Vincular btn_abrir y btn_cerrar para controlar la ventana
        Button btnAbrir = findViewById(R.id.btn_abrir);
        Button btnCerrar = findViewById(R.id.btn_cerrar);

        btnAbrir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conexion_ESP.getInstancia(ModoManual.this).abrir(ModoManual.this);
            }
        });

        btnCerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conexion_ESP.getInstancia(ModoManual.this).cerrar(ModoManual.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar esta actividad como callback para recibir los mensajes de MQTT
        conexion_ESP.getInstancia(this).setCallback(this);

        // Iniciar la detección de Shake al volver a la pantalla
        conexion_ESP.getInstancia(this).iniciarDeteccionShake(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detener la detección de Shake para evitar uso innecesario de sensores
        conexion_ESP.getInstancia(this).detenerDeteccionShake();
    }

    @Override
    public void onEstadoConexionCambiado(boolean conectado) {
        // Manejar cambios de estado de la conexión si es necesario
    }

    @Override
    public void onMensajeRecibido(String topic, String message) {
        runOnUiThread(() -> {
            if (topic.equals("/ventana/estado")) {
                // Abstraer la lógica del estado de la ventana en FuncionesGenericas
                FuncionesGenericas.actualizarEstadoVentana(this, tvEstadoVentana, message);
            } else if (topic.equals("/ventana/modo")) {
                // Abstraer la lógica del modo en FuncionesGenericas
                FuncionesGenericas.actualizarModoActual(this, tvModoActual, message);
            }
        });
    }
}