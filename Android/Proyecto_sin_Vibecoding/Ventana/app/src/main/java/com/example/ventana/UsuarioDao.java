package com.example.ventana;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert
    void insertar(Usuario usuario);
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :user LIMIT 1")
    Usuario obtenerPorNombre(String user);
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :user AND contrasena = :pass LIMIT 1")
    Usuario login(String user, String pass);

    @Query("SELECT * FROM usuarios")
    List<Usuario> obtenerTodos();
}