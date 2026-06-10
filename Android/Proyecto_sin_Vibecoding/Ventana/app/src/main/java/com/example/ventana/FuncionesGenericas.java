package com.example.ventana;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class FuncionesGenericas {

    // 1. Configurar el saludo del usuario logueado en la pantalla
    public static void configurarSaludoUsuario(Activity activity, TextView tvSaludo) {
        if (tvSaludo != null) {
            String nombreUsuario = GestorSesion.obtenerUsuario(activity);
            tvSaludo.setText("Usuario: " + nombreUsuario);
        }
    }

    // 2. Configurar el botón de Salir (Cerrar sesión)
    public static void configurarBotonSalir(final Activity activity, Button btnSalir) {
        if (btnSalir != null) {
            btnSalir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GestorSesion.cerrarSesion(activity);
                }
            });
        }
    }

    // 3. Configurar el botón de Configuración
    public static void configurarBotonConfiguracion(final Activity activity, Button btnConfig, final Runnable onSaveCallback) {
        if (btnConfig != null) {
            btnConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    conexion_ESP.getInstancia(activity).mostrarDialogoConfiguracion(activity, onSaveCallback);
                }
            });
        }
    }

    // 4. Actualizar visualmente el estado de la ventana (Texto y Colores)
    public static void actualizarEstadoVentana(Activity activity, TextView tvEstado, String estado) {
        if (tvEstado != null) {
            tvEstado.setText(estado);
            if (estado.equals("ABRIENDO") || estado.equals("CERRANDO")) {
                tvEstado.setTextColor(ContextCompat.getColor(activity, R.color.estado_movimiento));
            } else {
                tvEstado.setTextColor(ContextCompat.getColor(activity, android.R.color.black));
            }
        }
    }

    // 5. Actualizar visualmente el modo actual (Texto y Colores)
    public static void actualizarModoActual(Activity activity, TextView tvModo, String modo) {
        if (tvModo != null) {
            tvModo.setText(modo);
            if (modo.equals("AUTOMATICO")) {
                tvModo.setTextColor(ContextCompat.getColor(activity, R.color.estado_automatico));
            } else {
                tvModo.setTextColor(ContextCompat.getColor(activity, R.color.estado_manual));
            }
        }
    }
}
