package com.example.ventana;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class GestorSesion {
    private static final String PREFS_NAME = "SesionUsuarioPrefs";
    private static final String KEY_USERNAME = "nombre_usuario";

    public static void guardarUsuario(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USERNAME, username);
        editor.apply(); // Guarda de forma asíncrona
    }

    public static String obtenerUsuario(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "Invitado");
    }

    public static void cerrarSesion(Context context) {
        // Borramos los datos almacenados
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        // Redirigimos a la pantalla de Login (MainActivity)
        Intent intent = new Intent(context, MainActivity.class);

        // Limpiamos el historial de navegación para que no pueda volver atrás con el botón del celular
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        context.startActivity(intent);
        // Si el contexto actual es una Activity, la cerramos
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}
