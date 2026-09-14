package com.example.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "empleado_salario")
public class EmpleadoSalario {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String cedula;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "pago_hora_normal", nullable = false)
    private Double pagoHoraNormal = 15000.0; // Tarifa por defecto (Gs. por hora)

    @Column(name = "pago_hora_extra", nullable = false)
    private Double pagoHoraExtra = 22500.0; // Tarifa extra por defecto (1.5x)

    // Constructors
    public EmpleadoSalario() {
    }

    public EmpleadoSalario(String cedula, String nombre, Double pagoHoraNormal, Double pagoHoraExtra) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.pagoHoraNormal = pagoHoraNormal;
        this.pagoHoraExtra = pagoHoraExtra;
    }

    // Getters and Setters
    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPagoHoraNormal() {
        return pagoHoraNormal;
    }

    public void setPagoHoraNormal(Double pagoHoraNormal) {
        this.pagoHoraNormal = pagoHoraNormal;
    }

    public Double getPagoHoraExtra() {
        return pagoHoraExtra;
    }

    public void setPagoHoraExtra(Double pagoHoraExtra) {
        this.pagoHoraExtra = pagoHoraExtra;
    }
}
