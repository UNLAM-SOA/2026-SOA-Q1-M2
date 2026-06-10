package com.example.ventana;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Usuario.class}, version = 1, exportSchema = false)
public abstract class AppBaseDatos extends RoomDatabase {
    public abstract UsuarioDao usuarioDao();
    private static volatile AppBaseDatos INSTANCE;
    public static AppBaseDatos getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppBaseDatos.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppBaseDatos.class, "ventana_inteligente.db")
                            // ¡Ojo! En producción usarías migraciones, esto es para desarrollo:
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
