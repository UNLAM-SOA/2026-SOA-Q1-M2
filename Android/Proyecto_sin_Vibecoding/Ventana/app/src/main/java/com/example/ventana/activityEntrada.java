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
import android.widget.LinearLayout;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class ActivityEntrada extends AppCompatActivity {

    // Declaramos las variables para los componentes visuales
    private TextInputEditText etUser, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvCrearUsuario;

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
        tvCrearUsuario = findViewById(R.id.tvCrearUsuario);


        // Lógica del botón Entrar
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

        tvCrearUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoCrearUsuario();
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
        // 1. Validación rápida del admin hardcodeado
        if (usuario.equals("admin") && password.equals("admin")) {
            iniciarSesionExitosa(usuario);
            return;
        }
        // 2. Si no es el admin, buscamos en la base de datos (en un hilo de fondo)
        new Thread(() -> {
            AppBaseDatos db = AppBaseDatos.getInstance(ActivityEntrada.this);
            // Usamos el método login del DAO que busca por usuario y contraseña
            Usuario usuarioDb = db.usuarioDao().login(usuario, password);
            if (usuarioDb != null) {
                // Si el usuario existe en la DB y la contraseña coincide
                runOnUiThread(() -> {
                    iniciarSesionExitosa(usuario);
                });
            } else {
                // Si no existe o la contraseña es incorrecta
                runOnUiThread(() -> {
                    Toast.makeText(ActivityEntrada.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    private void iniciarSesionExitosa(String nombreUsuario) {
        Toast.makeText(this, "¡Ingreso exitoso! Bienvenido", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ActivityEntrada.this, ModoAutomatico.class);
        startActivity(intent);
        GestorSesion.guardarUsuario(ActivityEntrada.this, nombreUsuario);

        // Cerramos esta pantalla para que no se pueda volver atrás con el botón retroceder del celular
        finish();
    }
    private void mostrarDialogoCrearUsuario() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Crear Nuevo Usuario");
        // Creamos un contenedor vertical dinámico para colocar los inputs
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        // Input para el nombre de usuario
        final EditText inputUser = new EditText(this);
        inputUser.setHint("Nombre de usuario");
        inputUser.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(inputUser);
        // Input para la contraseña
        final EditText inputPassword = new EditText(this);
        inputPassword.setHint("Contraseña");
        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        // Agregamos un poco de espacio superior al input de la contraseña
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 24, 0, 0);
        inputPassword.setLayoutParams(params);
        layout.addView(inputPassword);
        builder.setView(layout);
        // Botón Confirmar / Crear
        builder.setPositiveButton("Crear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String user = inputUser.getText().toString().trim();
                String pass = inputPassword.getText().toString().trim();
                // Validación: campos vacíos
                if (user.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(ActivityEntrada.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Realizamos la validación y el guardado en un hilo de fondo (Thread)
                new Thread(() -> {
                    AppBaseDatos db = AppBaseDatos.getInstance(ActivityEntrada.this);
                    Usuario usuarioExistente = db.usuarioDao().obtenerPorNombre(user);
                    if (usuarioExistente != null) {
                        // Si ya existe, avisamos en pantalla
                        runOnUiThread(() -> {
                            Toast.makeText(ActivityEntrada.this, "El usuario ya existe", Toast.LENGTH_LONG).show();
                        });
                    } else {
                        // Si no existe, lo creamos y lo guardamos
                        Usuario nuevoUsuario = new Usuario();
                        nuevoUsuario.nombreUsuario = user;
                        nuevoUsuario.contrasena = pass;
                        db.usuarioDao().insertar(nuevoUsuario);
                        runOnUiThread(() -> {
                            Toast.makeText(ActivityEntrada.this, "Usuario creado con éxito", Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
            }
        });
        // Botón Cancelar
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel(); // Cierra el pop-up
            }
        });
        // Mostramos el diálogo en pantalla
        builder.show();
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
                    Toast.makeText(ActivityEntrada.this, "Correo enviado a: " + email, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ActivityEntrada.this, "Debes ingresar un correo válido", Toast.LENGTH_SHORT).show();
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