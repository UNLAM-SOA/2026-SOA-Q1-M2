package com.example.ventana;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class modoManual extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modo_manual);

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
                // Notificamos al broker MQTT el cambio de modo
                MqttManager.getInstance().publish("/ventana/modo", "AUTOMATICO");

                // Creamos el salto de regreso a MainActivity
                Intent intent = new Intent(modoManual.this, MainActivity.class);
                startActivity(intent);

                // Destruimos modoManual para que no quede de fondo
                finish();
            }
        });

        // NOTA: Acá abajo podés vincular btn_abrir y btn_cerrar
        // para controlar los relés o motores de la ventana por MQTT más adelante.
    }
}