package com.example.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "marcacion", indexes = {
    @Index(name = "idx_marcacion_fecha", columnList = "fecha"),
    @Index(name = "idx_marcacion_cedula", columnList = "cedula"),
    @Index(name = "idx_marcacion_nombre", columnList = "nombre")
})
public class Marcacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String cedula;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada;

    @Column(name = "hora_salida_almuerzo")
    private LocalTime horaSalidaAlmuerzo;

    @Column(name = "hora_entrada_almuerzo")
    private LocalTime horaEntradaAlmuerzo;

    @Column(name = "hora_salida_laboral")
    private LocalTime horaSalidaLaboral;

    // Calculated metrics
    @Column(name = "minutos_atraso_entrada")
    private Integer minutosAtrasoEntrada;

    @Column(name = "minutos_atraso_almuerzo")
    private Integer minutosAtrasoAlmuerzo;

    @Column(name = "minutos_salida_temprana_almuerzo")
    private Integer minutosSalidaTempranaAlmuerzo;

    @Column(name = "minutos_salida_temprana_laboral")
    private Integer minutosSalidaTempranaLaboral;

    @Column(name = "horas_trabajadas")
    private Double horasTrabajadas;

    @Column(name = "horas_extras")
    private Double horasExtras;

    @Column(length = 50)
    private String estado;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Column(name = "usuario_modificacion", length = 100)
    private String usuarioModificacion;

    // Constructors
    public Marcacion() {
    }

    public Marcacion(Long id, String cedula, String nombre, LocalDate fecha, LocalTime horaEntrada, 
                     LocalTime horaSalidaAlmuerzo, LocalTime horaEntradaAlmuerzo, LocalTime horaSalidaLaboral, 
                     Integer minutosAtrasoEntrada, Integer minutosAtrasoAlmuerzo, 
                     Integer minutosSalidaTempranaAlmuerzo, Integer minutosSalidaTempranaLaboral, 
                     Double horasTrabajadas, Double horasExtras, String estado) {
        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.horaSalidaAlmuerzo = horaSalidaAlmuerzo;
        this.horaEntradaAlmuerzo = horaEntradaAlmuerzo;
        this.horaSalidaLaboral = horaSalidaLaboral;
        this.minutosAtrasoEntrada = minutosAtrasoEntrada;
        this.minutosAtrasoAlmuerzo = minutosAtrasoAlmuerzo;
        this.minutosSalidaTempranaAlmuerzo = minutosSalidaTempranaAlmuerzo;
        this.minutosSalidaTempranaLaboral = minutosSalidaTempranaLaboral;
        this.horasTrabajadas = horasTrabajadas;
        this.horasExtras = horasExtras;
        this.estado = estado;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(LocalTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public LocalTime getHoraSalidaAlmuerzo() {
        return horaSalidaAlmuerzo;
    }

    public void setHoraSalidaAlmuerzo(LocalTime horaSalidaAlmuerzo) {
        this.horaSalidaAlmuerzo = horaSalidaAlmuerzo;
    }

    public LocalTime getHoraEntradaAlmuerzo() {
        return horaEntradaAlmuerzo;
    }

    public void setHoraEntradaAlmuerzo(LocalTime horaEntradaAlmuerzo) {
        this.horaEntradaAlmuerzo = horaEntradaAlmuerzo;
    }

    public LocalTime getHoraSalidaLaboral() {
        return horaSalidaLaboral;
    }

    public void setHoraSalidaLaboral(LocalTime horaSalidaLaboral) {
        this.horaSalidaLaboral = horaSalidaLaboral;
    }

    public Integer getMinutosAtrasoEntrada() {
        return minutosAtrasoEntrada;
    }

    public void setMinutosAtrasoEntrada(Integer minutosAtrasoEntrada) {
        this.minutosAtrasoEntrada = minutosAtrasoEntrada;
    }

    public Integer getMinutosAtrasoAlmuerzo() {
        return minutosAtrasoAlmuerzo;
    }

    public void setMinutosAtrasoAlmuerzo(Integer minutosAtrasoAlmuerzo) {
        this.minutosAtrasoAlmuerzo = minutosAtrasoAlmuerzo;
    }

    public Integer getMinutosSalidaTempranaAlmuerzo() {
        return minutosSalidaTempranaAlmuerzo;
    }

    public void setMinutosSalidaTempranaAlmuerzo(Integer minutosSalidaTempranaAlmuerzo) {
        this.minutosSalidaTempranaAlmuerzo = minutosSalidaTempranaAlmuerzo;
    }

    public Integer getMinutosSalidaTempranaLaboral() {
        return minutosSalidaTempranaLaboral;
    }

    public void setMinutosSalidaTempranaLaboral(Integer minutosSalidaTempranaLaboral) {
        this.minutosSalidaTempranaLaboral = minutosSalidaTempranaLaboral;
    }

    public Double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(Double horasTrabajadas) {
        this.horasTrabajadas = horasTrabajadas;
    }

    public Double getHorasExtras() {
        return horasExtras;
    }

    public void setHorasExtras(Double horasExtras) {
        this.horasExtras = horasExtras;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public String getHorasTrabajadasFormateadas() {
        if (horasTrabajadas == null || horasTrabajadas == 0.0) {
            return "0hs.0min.";
        }
        long totalMinutos = Math.round(horasTrabajadas * 60.0);
        long hs = totalMinutos / 60;
        long mins = totalMinutos % 60;
        return hs + "hs." + mins + "min.";
    }

    public String getHorasExtrasFormateadas() {
        if (horasExtras == null || horasExtras == 0.0) {
            return "0hs.0min.";
        }
        long totalMinutos = Math.round(horasExtras * 60.0);
        long hs = totalMinutos / 60;
        long mins = totalMinutos % 60;
        return hs + "hs." + mins + "min.";
    }

    public String getHorasTotalesFormateadas() {
        double tTrabajadas = horasTrabajadas != null ? horasTrabajadas : 0.0;
        double tExtras = horasExtras != null ? horasExtras : 0.0;
        double total = tTrabajadas + tExtras;
        if (total == 0.0) {
            return "0hs.0min.";
        }
        long totalMinutos = Math.round(total * 60.0);
        long hs = totalMinutos / 60;
        long mins = totalMinutos % 60;
        return hs + "hs." + mins + "min.";
    }

    public double getHorasTotales() {
        double tTrabajadas = horasTrabajadas != null ? horasTrabajadas : 0.0;
        double tExtras = horasExtras != null ? horasExtras : 0.0;
        return tTrabajadas + tExtras;
    }
}
