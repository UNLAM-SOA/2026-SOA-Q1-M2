package com.example.ventana;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
                    ConexionESP.getInstancia(activity).mostrarDialogoConfiguracion(activity, onSaveCallback);
                }
            });
        }
    }

    // 4. Actualizar visualmente el estado de la ventana (Texto y Colores)
    public static void actualizarEstadoVentana(Activity activity, TextView tvEstado, String estado, Button btnEmergencia) {
        if (tvEstado != null) {
            tvEstado.setText(estado);
            
            // Limpiar fondo
            tvEstado.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            android.view.View contenedorPadre = (android.view.View) tvEstado.getParent();
            if (contenedorPadre != null) {
                contenedorPadre.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            if (estado.equals("ABRIENDO") || estado.equals("CERRANDO")) {
                tvEstado.setTextColor(ContextCompat.getColor(activity, R.color.estado_movimiento));
            } else if (estado.equalsIgnoreCase("BLOQUEADO")) {
                // Texto en rojo
                tvEstado.setTextColor(ContextCompat.getColor(activity, R.color.color_rojo));
            } else {
                tvEstado.setTextColor(ContextCompat.getColor(activity, android.R.color.black));
            }
        }

        if (btnEmergencia != null) {
            if (estado.equalsIgnoreCase("BLOQUEADO")) {
                btnEmergencia.setText("DESBLOQUEAR");
                btnEmergencia.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                btnEmergencia.setTextColor(Color.BLACK);
            } else {
                btnEmergencia.setText("EMERGENCIA");
                btnEmergencia.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                btnEmergencia.setTextColor(Color.WHITE);
            }
        }
    }
}
