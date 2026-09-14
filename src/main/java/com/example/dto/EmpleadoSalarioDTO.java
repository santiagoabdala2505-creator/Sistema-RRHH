package com.example.dto;

public class EmpleadoSalarioDTO {
    private String cedula;
    private String nombre;
    private Double pagoHoraNormal;
    private Double pagoHoraExtra;
    private Double totalHorasNormales;
    private Double totalHorasExtras;

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

    public Double getTotalHorasNormales() {
        return totalHorasNormales;
    }

    public void setTotalHorasNormales(Double totalHorasNormales) {
        this.totalHorasNormales = totalHorasNormales;
    }

    public Double getTotalHorasExtras() {
        return totalHorasExtras;
    }

    public void setTotalHorasExtras(Double totalHorasExtras) {
        this.totalHorasExtras = totalHorasExtras;
    }

    // Helper formatting methods
    public String getHorasNormalesFormateadas() {
        if (totalHorasNormales == null || totalHorasNormales == 0.0) {
            return "0hs.0min.";
        }
        long totalMinutos = Math.round(totalHorasNormales * 60.0);
        return (totalMinutos / 60) + "hs." + (totalMinutos % 60) + "min.";
    }

    public String getHorasExtrasFormateadas() {
        if (totalHorasExtras == null || totalHorasExtras == 0.0) {
            return "0hs.0min.";
        }
        long totalMinutos = Math.round(totalHorasExtras * 60.0);
        return (totalMinutos / 60) + "hs." + (totalMinutos % 60) + "min.";
    }

    public Double getSalarioNormal() {
        return (totalHorasNormales != null ? totalHorasNormales : 0.0) * (pagoHoraNormal != null ? pagoHoraNormal : 0.0);
    }

    public Double getSalarioExtra() {
        return (totalHorasExtras != null ? totalHorasExtras : 0.0) * (pagoHoraExtra != null ? pagoHoraExtra : 0.0);
    }

    public Double getSalarioTotal() {
        return getSalarioNormal() + getSalarioExtra();
    }

    public String getSalarioNormalFormateado() {
        return formatCurrency(getSalarioNormal());
    }

    public String getSalarioExtraFormateado() {
        return formatCurrency(getSalarioExtra());
    }

    public String getSalarioTotalFormateado() {
        return formatCurrency(getSalarioTotal());
    }

    private String formatCurrency(Double value) {
        if (value == null) return "0 Gs.";
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0");
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        df.setDecimalFormatSymbols(symbols);
        return df.format(value) + " Gs.";
    }
}
