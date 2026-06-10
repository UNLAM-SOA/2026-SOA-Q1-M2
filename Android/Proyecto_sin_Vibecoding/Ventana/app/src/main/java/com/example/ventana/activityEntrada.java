package com.example.ventana;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class activityEntrada extends AppCompatActivity {

    // Declaramos las variables para los componentes visuales
    private TextInputEditText etUser, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrada);

        // Configuración de los márgenes del sistema (EdgeToEdge)
        // Asegurate de que el ScrollView en tu XML tenga el id "main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Enlazamos los componentes del XML con Java
        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Lógica del botón Entrar
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarIngreso();
            }
        });

        // Lógica del texto "Olvidé mi contraseña"
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoRecuperarClave();
            }
        });
    }

    // Método para validar el admin admin hardcodeado
    private void validarIngreso() {
        String usuario = etUser.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación hardcodeada
        if (usuario.equals("admin") && password.equals("admin")) {
            Toast.makeText(this, "¡Ingreso exitoso! Bienvenido", Toast.LENGTH_SHORT).show();

            // Creamos el Intent para pasar desde esta actividad a MainActivity
            Intent intent = new Intent(activityEntrada.this, MainActivity.class);
            startActivity(intent);

            // Destruimos esta activity para que no quede en el historial de navegación hacia atrás
            finish();

        } else {
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para mostrar el cuadro de diálogo (Pop-up) para el email
    private void mostrarDialogoRecuperarClave() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recuperar Contraseña");
        builder.setMessage("Ingresa tu correo electrónico para enviarte las instrucciones:");

        // Creamos un campo de texto dinámico para que el usuario escriba su mail
        final EditText inputEmail = new EditText(this);
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setHint("ejemplo@correo.com");

        // Le damos un poco de margen interno al campo de texto dentro del diálogo
        inputEmail.setPadding(40, 30, 40, 30);
        builder.setView(inputEmail);

        // Botón Confirmar / Enviar
        builder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = inputEmail.getText().toString().trim();
                if (!email.isEmpty()) {
                    // Por ahora solo emula la acción con un cartelito en pantalla
                    Toast.makeText(activityEntrada.this, "Correo enviado a: " + email, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activityEntrada.this, "Debes ingresar un correo válido", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Botón Cancelar
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel(); // Cierra el pop-up sin hacer nada
            }
        });

        // Mostramos el cuadro en pantalla
        builder.show();
    }
}