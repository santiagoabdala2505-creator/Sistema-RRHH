package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "planilla_detalle")
public class PlanillaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabecera_id", nullable = false)
    @JsonIgnore
    private PlanillaCabecera cabecera;

    @Column(nullable = false, length = 50)
    private String cedula;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 20)
    private String cotizante = "jornalero"; // general, jornalero

    private Double lunes = 0.0;
    private Double martes = 0.0;
    private Double miercoles = 0.0;
    private Double jueves = 0.0;
    private Double viernes = 0.0;
    private Double sabado = 0.0;
    private Double domingo = 0.0; // Do

    private Double jornal = 120000.0;

    @Column(name = "he_lunes")
    private Double heLunes = 0.0;
    @Column(name = "he_martes")
    private Double heMartes = 0.0;
    @Column(name = "he_miercoles")
    private Double heMiercoles = 0.0;
    @Column(name = "he_jueves")
    private Double heJueves = 0.0;
    @Column(name = "he_viernes")
    private Double heViernes = 0.0;
    @Column(name = "he_sabado")
    private Double heSabado = 0.0;

    @Column(name = "cantidad_hora_sem")
    private Double cantidadHoraSem = 0.0;

    @Column(name = "desc_manual")
    private Double descManual = 0.0;

    private Double colaboracion = 0.0;

    @Column(name = "metodo_pago", length = 20)
    private String metodoPago = "EFECTIVO"; // EFECTIVO, BANCO, CHEQUE

    private Double descuentos = 0.0;
    private Double anticipo = 0.0;

    @Column(name = "total_dias")
    private Double totalDias = 0.0;

    @Column(name = "importe_domingo")
    private Double importeDomingo = 0.0;

    @Column(name = "total_horas_extras")
    private Double totalHorasExtras = 0.0;

    @Column(name = "gross_pay")
    private Double grossPay = 0.0;

    @Column(name = "ips")
    private Double ips = 0.0;

    @Column(name = "neto_tarjeta")
    private Double netoTarjeta = 0.0;

    @Column(name = "neto_tesoreria")
    private Double netoTesoreria = 0.0;

    @Column(name = "importe_hora_extra")
    private Double importeHoraExtra = 0.0;

    // Constructors
    public PlanillaDetalle() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanillaCabecera getCabecera() {
        return cabecera;
    }

    public void setCabecera(PlanillaCabecera cabecera) {
        this.cabecera = cabecera;
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

    public String getCotizante() {
        return cotizante;
    }

    public void setCotizante(String cotizante) {
        this.cotizante = cotizante;
    }

    public Double getLunes() {
        return lunes;
    }

    public void setLunes(Double lunes) {
        this.lunes = lunes;
    }

    public Double getMartes() {
        return martes;
    }

    public void setMartes(Double martes) {
        this.martes = martes;
    }

    public Double getMiercoles() {
        return miercoles;
    }

    public void setMiercoles(Double miercoles) {
        this.miercoles = miercoles;
    }

    public Double getJueves() {
        return jueves;
    }

    public void setJueves(Double jueves) {
        this.jueves = jueves;
    }

    public Double getViernes() {
        return viernes;
    }

    public void setViernes(Double viernes) {
        this.viernes = viernes;
    }

    public Double getSabado() {
        return sabado;
    }

    public void setSabado(Double sabado) {
        this.sabado = sabado;
    }

    public Double getDomingo() {
        return domingo;
    }

    public void setDomingo(Double domingo) {
        this.domingo = domingo;
    }

    public Double getJornal() {
        return jornal;
    }

    public void setJornal(Double jornal) {
        this.jornal = jornal;
    }

    public Double getHeLunes() {
        return heLunes;
    }

    public void setHeLunes(Double heLunes) {
        this.heLunes = heLunes;
    }

    public Double getHeMartes() {
        return heMartes;
    }

    public void setHeMartes(Double heMartes) {
        this.heMartes = heMartes;
    }

    public Double getHeMiercoles() {
        return heMiercoles;
    }

    public void setHeMiercoles(Double heMiercoles) {
        this.heMiercoles = heMiercoles;
    }

    public Double getHeJueves() {
        return heJueves;
    }

    public void setHeJueves(Double heJueves) {
        this.heJueves = heJueves;
    }

    public Double getHeViernes() {
        return heViernes;
    }

    public void setHeViernes(Double heViernes) {
        this.heViernes = heViernes;
    }

    public Double getHeSabado() {
        return heSabado;
    }

    public void setHeSabado(Double heSabado) {
        this.heSabado = heSabado;
    }

    public Double getCantidadHoraSem() {
        return cantidadHoraSem;
    }

    public void setCantidadHoraSem(Double cantidadHoraSem) {
        this.cantidadHoraSem = cantidadHoraSem;
    }

    public Double getDescManual() {
        return descManual;
    }

    public void setDescManual(Double descManual) {
        this.descManual = descManual;
    }

    public Double getColaboracion() {
        return colaboracion;
    }

    public void setColaboracion(Double colaboracion) {
        this.colaboracion = colaboracion;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Double getDescuentos() {
        return descuentos;
    }

    public void setDescuentos(Double descuentos) {
        this.descuentos = descuentos;
    }

    public Double getAnticipo() {
        return anticipo;
    }

    public void setAnticipo(Double anticipo) {
        this.anticipo = anticipo;
    }

    public Double getTotalDias() {
        return totalDias;
    }

    public void setTotalDias(Double totalDias) {
        this.totalDias = totalDias;
    }

    public Double getImporteDomingo() {
        return importeDomingo;
    }

    public void setImporteDomingo(Double importeDomingo) {
        this.importeDomingo = importeDomingo;
    }

    public Double getTotalHorasExtras() {
        return totalHorasExtras;
    }

    public void setTotalHorasExtras(Double totalHorasExtras) {
        this.totalHorasExtras = totalHorasExtras;
    }

    public Double getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(Double grossPay) {
        this.grossPay = grossPay;
    }

    public Double getIps() {
        return ips;
    }

    public void setIps(Double ips) {
        this.ips = ips;
    }

    public Double getNetoTarjeta() {
        return netoTarjeta;
    }

    public void setNetoTarjeta(Double netoTarjeta) {
        this.netoTarjeta = netoTarjeta;
    }

    public Double getNetoTesoreria() {
        return netoTesoreria;
    }

    public void setNetoTesoreria(Double netoTesoreria) {
        this.netoTesoreria = netoTesoreria;
    }

    public Double getImporteHoraExtra() {
        return importeHoraExtra;
    }

    public void setImporteHoraExtra(Double importeHoraExtra) {
        this.importeHoraExtra = importeHoraExtra;
    }
}
