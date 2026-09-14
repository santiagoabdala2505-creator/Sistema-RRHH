package com.example.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planilla_cabecera")
public class PlanillaCabecera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "tipo_carpeta", length = 30)
    private String tipoCarpeta = "JORNALEROS"; // JORNALEROS, MENSUALEROS

    @Column(name = "subcarpeta_mes", length = 30)
    private String subcarpetaMes; // ENERO, FEBRERO, ...

    @Column(name = "total_neto")
    private Double totalNeto = 0.0;

    @OneToMany(mappedBy = "cabecera", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PlanillaDetalle> detalles = new ArrayList<>();

    // Constructors
    public PlanillaCabecera() {
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

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getTipoCarpeta() {
        return tipoCarpeta;
    }

    public void setTipoCarpeta(String tipoCarpeta) {
        this.tipoCarpeta = tipoCarpeta;
    }

    public String getSubcarpetaMes() {
        return subcarpetaMes;
    }

    public void setSubcarpetaMes(String subcarpetaMes) {
        this.subcarpetaMes = subcarpetaMes;
    }

    public Double getTotalNeto() {
        return totalNeto;
    }

    public void setTotalNeto(Double totalNeto) {
        this.totalNeto = totalNeto;
    }

    public List<PlanillaDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<PlanillaDetalle> detalles) {
        this.detalles = detalles;
    }
}
