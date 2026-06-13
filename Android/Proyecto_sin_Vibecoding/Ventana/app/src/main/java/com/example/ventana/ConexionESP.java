package com.example.ventana;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

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

    // Interfaz para notificar cambios de estado o recepción de datos
    public interface CallbackConexion {
        void onEstadoConexionCambiado(boolean conectado);
        void onMensajeRecibido(String topico, String mensaje);
    }

    private CallbackConexion callback;

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
        Log.d(TAG, "Configuración cargada: Host=" + host + ", Port=" + port + ", User=" + user);
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
        Log.d(TAG, "Configuración guardada en SharedPreferences y actualizada en ConexionESP");
    }

    public void setCallback(CallbackConexion callback) {
        this.callback = callback;
    }

    public void conectar() {
        Log.d(TAG, "Conectando al ESP32 a través de MQTT en: " + host + ":" + port);
        
        // Vincular con MqttManager para que use la misma conexión/simulación
        MqttManager.getInstance().setCallbackListener(new MqttManager.MqttCallbackListener() {
            @Override
            public void onConnectionStatusChanged(boolean isConnected) {
                if (callback != null) {
                    callback.onEstadoConexionCambiado(isConnected);
                }
            }

            @Override
            public void onMessageReceived(String topic, String message) {
                if (callback != null) {
                    callback.onMensajeRecibido(topic, message);
                }
            }
        });
        
        MqttManager.getInstance().connect(host + ":" + port, "AndroidClient_" + System.currentTimeMillis(), user, password);
    }

    public void suscribir(String topico) {
        Log.d(TAG, "Suscribiéndose al tópico: " + topico);
        // Aquí se implementará la suscripción real a MQTT.
    }

    public void publicar(String topico, String mensaje) {
        Log.d(TAG, "Publicando mensaje en " + topico + ": " + mensaje);
        MqttManager.getInstance().publish(topico, mensaje);
    }

    public void abrir(Context context) {
        Log.d(TAG, "Enviando comando para abrir la ventana...");
        publicar("/ventana/comando", "ABRIR");
        android.widget.Toast.makeText(context, "Comando enviado", android.widget.Toast.LENGTH_SHORT).show();
    }

    public void cerrar(Context context) {
        Log.d(TAG, "Enviando comando para cerrar la ventana...");
        publicar("/ventana/comando", "CERRAR");
        android.widget.Toast.makeText(context, "Comando enviado", android.widget.Toast.LENGTH_SHORT).show();
    }

    public void accionShake(Context context) {
        Log.d(TAG, "Agitación (Shake) detectada. Enviando comando al ESP32...");
        publicar("/ventana/comando", "SHAKE");
        android.widget.Toast.makeText(context, "Shake detectado: Acción enviada", android.widget.Toast.LENGTH_SHORT).show();
    }

    public void iniciarDeteccionShake(Context context) {
        this.currentContext = context;
        try {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometer != null) {
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                    Log.d(TAG, "Detección de Shake iniciada");
                } else {
                    Log.w(TAG, "Acelerómetro no disponible en este dispositivo");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar detección de Shake", e);
        }
    }

    public void detenerDeteccionShake() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Detección de Shake detenida");
        }
        this.currentContext = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Magnitud de la aceleración
            float gForce = (float) Math.sqrt(x * x + y * y + z * z);
            
            // Restar la gravedad terrestre
            float netForce = Math.abs(gForce - SensorManager.GRAVITY_EARTH);

            if (netForce > SHAKE_THRESHOLD_ACCEL) {
                long now = System.currentTimeMillis();
                
                // Si el evento anterior fue hace demasiado tiempo, reseteamos el contador
                if (lastTimeShake == 0 || (now - lastTimeShake > SHAKE_TIME_LAPSE)) {
                    shakeCount = 0;
                    lastTimeShake = now;
                }
                
                shakeCount++;
                lastTimeShake = now;

                Log.d(TAG, "Sacudida detectada (" + shakeCount + "/" + SHAKE_COUNT_TRIGGER + "). Fuerza: " + netForce);

                if (shakeCount >= SHAKE_COUNT_TRIGGER) {
                    shakeCount = 0;
                    lastTimeShake = 0;
                    if (currentContext != null) {
                        accionShake(currentContext);
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se requiere para esta implementación
    }

    // Getters y Setters
    public String getHost() { return host; }
    public String getPort() { return port; }
    public String getUser() { return user; }
    public String getPassword() { return password; }

    public void mostrarDialogoConfiguracion(final Context context, final Runnable onSaveCallback) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Configuración de Conexión");

        // Contenedor vertical
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // Input para Host
        final android.widget.EditText inputHost = new android.widget.EditText(context);
        inputHost.setHint("Host");
        inputHost.setText(this.getHost());
        inputHost.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(inputHost);

        // Input para Port
        final android.widget.EditText inputPort = new android.widget.EditText(context);
        inputPort.setHint("Port");
        inputPort.setText(this.getPort());
        inputPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        android.widget.LinearLayout.LayoutParams paramsPort = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsPort.setMargins(0, 24, 0, 0);
        inputPort.setLayoutParams(paramsPort);
        layout.addView(inputPort);

        // Input para User
        final android.widget.EditText inputUser = new android.widget.EditText(context);
        inputUser.setHint("User");
        inputUser.setText(this.getUser());
        inputUser.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        android.widget.LinearLayout.LayoutParams paramsUser = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsUser.setMargins(0, 24, 0, 0);
        inputUser.setLayoutParams(paramsUser);
        layout.addView(inputUser);

        // Input para Password
        final android.widget.EditText inputPassword = new android.widget.EditText(context);
        inputPassword.setHint("Password");
        inputPassword.setText(this.getPassword());
        inputPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        android.widget.LinearLayout.LayoutParams paramsPassword = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsPassword.setMargins(0, 24, 0, 0);
        inputPassword.setLayoutParams(paramsPassword);
        layout.addView(inputPassword);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String hostVal = inputHost.getText().toString().trim();
                String portVal = inputPort.getText().toString().trim();
                String userVal = inputUser.getText().toString().trim();
                String passVal = inputPassword.getText().toString().trim();

                if (hostVal.isEmpty() || portVal.isEmpty()) {
                    android.widget.Toast.makeText(context, "Host y Puerto son requeridos", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Guardar la configuración
                guardarConfiguracion(context, hostVal, portVal, userVal, passVal);
                android.widget.Toast.makeText(context, "Configuración guardada", android.widget.Toast.LENGTH_SHORT).show();

                // Reconectar con los nuevos datos
                conectar();

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
            }
        });

        builder.setNegativeButton("Cancelar", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
