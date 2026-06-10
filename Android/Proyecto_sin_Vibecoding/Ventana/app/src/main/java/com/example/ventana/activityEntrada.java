package com.example.ventana;


import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class activityEntrada extends AppCompatActivity {

    private TextInputEditText etUser, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvcrearUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrada);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvcrearUsuario = findViewById(R.id.tvCrearUsuario);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarIngreso();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoRecuperarClave();
            }
        });
        tvcrearUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoCrearUsuario();
            }
        });
    }

    private void validarIngreso() {
        String usuario = etUser.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (usuario.equals("admin") && password.equals("admin")) {
            irAMainActivity( "Bienvenido ADMIN");
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AppBaseDatos db = AppBaseDatos.getInstance(getApplicationContext());
                    Usuario usuarioEncontrado = db.usuarioDao().login(usuario, password);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (usuarioEncontrado != null) {
                                irAMainActivity("¡Ingreso exitoso! Bienvenido " + usuarioEncontrado.nombreUsuario);
                            } else {
                                Toast.makeText(activityEntrada.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }).start();
        }
    }
    private void irAMainActivity(String mensajeBienvenida) {
        Toast.makeText(this, mensajeBienvenida, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(activityEntrada.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private void mostrarDialogoRecuperarClave() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recuperar Contraseña");
        builder.setMessage("Ingresa tu correo electrónico:");

        final EditText inputEmail = new EditText(this);
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setHint("ejemplo@correo.com");
        builder.setView(inputEmail);

        builder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = inputEmail.getText().toString().trim();
                if (!email.isEmpty()) {
                    Toast.makeText(activityEntrada.this, "Correo enviado a: " + email, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activityEntrada.this, "Debes ingresar un correo válido", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


    private void mostrarDialogoCrearUsuario() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Crear Usuario");
        LinearLayout layoutContenedor = new LinearLayout(this);
        layoutContenedor.setOrientation(LinearLayout.VERTICAL);
        layoutContenedor.setPadding(50, 40, 50, 10);
        final EditText inputUsuario = new EditText(this);
        inputUsuario.setHint("Ingresa tu Usuario");
        inputUsuario.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layoutContenedor.addView(inputUsuario);

        final EditText inputClave = new EditText(this);
        inputClave.setHint("Ingresa tu contraseña");
        inputClave.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layoutContenedor.addView(inputClave);
        builder.setView(layoutContenedor);

        builder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String Usuario = inputUsuario.getText().toString().trim();
                String clave = inputClave.getText().toString().trim();
                if (!Usuario.isEmpty() && !clave.isEmpty()) {
                    Usuario nuevoUsuario = new Usuario();
                    nuevoUsuario.nombreUsuario = Usuario;
                    nuevoUsuario.contrasena = clave;

                    new Thread(new Runnable() { //hilo secundario para procesar los datos en la DB
                        @Override
                        public void run() {
                            AppBaseDatos db = AppBaseDatos.getInstance(getApplicationContext());
                            db.usuarioDao().insertar(nuevoUsuario);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Usuario '" + Usuario + "' creado con éxito", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(activityEntrada.this, "El usuario y/o contraseña no pueden ser vacios", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}