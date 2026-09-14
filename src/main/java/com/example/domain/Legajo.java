package com.example.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "legajo")
public class Legajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "archivo_datos", columnDefinition = "bytea", nullable = false)
    private byte[] archivoDatos;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    @Column(name = "usuario_subida", length = 100)
    private String usuarioSubida;

    // Constructors
    public Legajo() {
    }

    public Legajo(Long id, String nombre, String nombreArchivo, byte[] archivoDatos, LocalDateTime fechaSubida, String usuarioSubida) {
        this.id = id;
        this.nombre = nombre;
        this.nombreArchivo = nombreArchivo;
        this.archivoDatos = archivoDatos;
        this.fechaSubida = fechaSubida;
        this.usuarioSubida = usuarioSubida;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public byte[] getArchivoDatos() {
        return archivoDatos;
    }

    public void setArchivoDatos(byte[] archivoDatos) {
        this.archivoDatos = archivoDatos;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public String getUsuarioSubida() {
        return usuarioSubida;
    }

    public void setUsuarioSubida(String usuarioSubida) {
        this.usuarioSubida = usuarioSubida;
    }
}
