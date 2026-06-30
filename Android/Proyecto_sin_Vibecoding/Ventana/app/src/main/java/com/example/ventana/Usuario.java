package com.example.ventana;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nombreUsuario;
    public String contrasena;
}
