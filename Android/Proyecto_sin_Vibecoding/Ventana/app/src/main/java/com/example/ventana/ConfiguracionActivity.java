package com.example.ventana;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class ConfiguracionActivity extends AppCompatActivity {

    private TextInputEditText etHost, etPort, etUser, etPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        etHost = findViewById(R.id.et_host);
        etPort = findViewById(R.id.et_port);
        etUser = findViewById(R.id.et_user);
        etPass = findViewById(R.id.et_pass);
        Button btnGuardar = findViewById(R.id.btn_guardar);

        cargarConfiguracion();

        btnGuardar.setOnClickListener(v -> guardarConfiguracion());
    }

    private void cargarConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("ConfigMQTT", MODE_PRIVATE);
        etHost.setText(prefs.getString("host", "tcp://broker.emqx.io"));
        etPort.setText(prefs.getString("port", "1883"));
        etUser.setText(prefs.getString("user", ""));
        etPass.setText(prefs.getString("pass", ""));
    }

    private void guardarConfiguracion() {
        String host = etHost.getText().toString().trim();
        String port = etPort.getText().toString().trim();
        String user = etUser.getText().toString().trim();
        String pass = etPass.getText().toString().trim();

        if (host.isEmpty() || port.isEmpty()) {
            Toast.makeText(this, "Host y Puerto son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences("ConfigMQTT", MODE_PRIVATE).edit();
        editor.putString("host", host);
        editor.putString("port", port);
        editor.putString("user", user);
        editor.putString("pass", pass);
        editor.apply();

        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
        finish();
    }
}
