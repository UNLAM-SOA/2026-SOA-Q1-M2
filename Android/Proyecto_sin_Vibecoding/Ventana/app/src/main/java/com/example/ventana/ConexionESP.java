package com.example.ventana;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Clase de utilidad para gestionar la interacción con el ESP32.
 * Actúa como un puente entre la UI y el MqttService.
 */
public class ConexionESP implements SensorEventListener {
    private static final String TAG = "ConexionESP";
    private static ConexionESP instancia;

    private String host;
    private String port;
    private String user;
    private String password;

    // Variables para la detección del Shake
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD_ACCEL = 12.0f;
    private static final int SHAKE_TIME_LAPSE = 500;
    private static final int SHAKE_COUNT_TRIGGER = 3;
    private long lastTimeShake = 0;
    private int shakeCount = 0;
    private Context currentContext;

    private ConexionESP(Context context) {
        cargarConfiguracion(context);
    }

    public static synchronized ConexionESP getInstancia(Context context) {
        if (instancia == null) {
            instancia = new ConexionESP(context.getApplicationContext());
        }
        return instancia;
    }

    public void cargarConfiguracion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ConfigMQTT", Context.MODE_PRIVATE);
        this.host = prefs.getString("host", "tcp://broker.emqx.io");
        this.port = prefs.getString("port", "1883");
        this.user = prefs.getString("user", "");
        this.password = prefs.getString("pass", "");
        Log.d(TAG, "Configuración cargada");
    }

    public void guardarConfiguracion(Context context, String host, String port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;

        SharedPreferences prefs = context.getSharedPreferences("ConfigMQTT", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("host", host);
        editor.putString("port", port);
        editor.putString("user", user);
        editor.putString("pass", password);
        editor.apply();
        Log.d(TAG, "Configuración guardada");
    }

    /**
     * Inicia el proceso de conexión enviando un comando al MqttService.
     */
    public void conectar(Context context) {
        Log.d(TAG, "Solicitando conexión al servicio...");
        Intent intent = new Intent(context, MqttService.class);
        intent.setAction(MqttService.ACTION_CONNECT);
        intent.putExtra("host", host);
        intent.putExtra("port", port);
        intent.putExtra("user", user);
        intent.putExtra("pass", password);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Publica un mensaje genérico mediante el MqttService.
     */
    public void publicar(Context context, String topico, String mensaje) {
        Intent intent = new Intent(context, MqttService.class);
        intent.setAction(MqttService.ACTION_PUBLISH);
        intent.putExtra("topic", topico);
        intent.putExtra("message", mensaje);
        context.startService(intent);
    }

    public void abrir(Context context) {
        publicar(context, "/ventana/comando", "ABRIR");
        android.widget.Toast.makeText(context, "Enviando: ABRIR", android.widget.Toast.LENGTH_SHORT).show();
    }

    public void cerrar(Context context) {
        publicar(context, "/ventana/comando", "CERRAR");
        android.widget.Toast.makeText(context, "Enviando: CERRAR", android.widget.Toast.LENGTH_SHORT).show();
    }

    public void accionShake(Context context) {
        publicar(context, "/ventana/comando", "SHAKE");
        android.widget.Toast.makeText(context, "Shake: Comando enviado", android.widget.Toast.LENGTH_SHORT).show();
    }

    // --- Lógica de Sensores (Shake) ---

    public void iniciarDeteccionShake(Context context) {
        this.currentContext = context;
        try {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar detección de Shake", e);
        }
    }

    public void detenerDeteccionShake() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        this.currentContext = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gForce = (float) Math.sqrt(x * x + y * y + z * z);
            float netForce = Math.abs(gForce - SensorManager.GRAVITY_EARTH);

            if (netForce > SHAKE_THRESHOLD_ACCEL) {
                long now = System.currentTimeMillis();
                if (lastTimeShake == 0 || (now - lastTimeShake > SHAKE_TIME_LAPSE)) {
                    shakeCount = 0;
                    lastTimeShake = now;
                }
                shakeCount++;
                lastTimeShake = now;
                if (shakeCount >= SHAKE_COUNT_TRIGGER) {
                    shakeCount = 0;
                    lastTimeShake = 0;
                    if (currentContext != null) accionShake(currentContext);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public String getHost() { return host; }
    public String getPort() { return port; }
    public String getUser() { return user; }
    public String getPassword() { return password; }

    public void mostrarDialogoConfiguracion(final Context context, final Runnable onSaveCallback) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Configuración MQTT");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        final android.widget.EditText inputHost = new android.widget.EditText(context);
        inputHost.setHint("Broker (ej: tcp://broker.emqx.io)");
        inputHost.setText(this.getHost());
        layout.addView(inputHost);

        final android.widget.EditText inputPort = new android.widget.EditText(context);
        inputPort.setHint("Puerto (ej: 1883)");
        inputPort.setText(this.getPort());
        layout.addView(inputPort);

        final android.widget.EditText inputUser = new android.widget.EditText(context);
        inputUser.setHint("Usuario");
        inputUser.setText(this.getUser());
        layout.addView(inputUser);

        final android.widget.EditText inputPassword = new android.widget.EditText(context);
        inputPassword.setHint("Contraseña");
        inputPassword.setText(this.getPassword());
        inputPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputPassword);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String h = inputHost.getText().toString().trim();
            String p = inputPort.getText().toString().trim();
            String u = inputUser.getText().toString().trim();
            String pw = inputPassword.getText().toString().trim();

            if (h.isEmpty() || p.isEmpty()) {
                android.widget.Toast.makeText(context, "Broker y Puerto requeridos", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            guardarConfiguracion(context, h, p, u, pw);
            conectar(context);
            if (onSaveCallback != null) onSaveCallback.run();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
