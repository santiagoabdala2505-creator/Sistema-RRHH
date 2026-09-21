package com.example.service;

import com.example.domain.PlanillaCabecera;
import com.example.domain.PlanillaDetalle;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReciboSalarioExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(new Locale("de", "DE")));

    // Colores basados en el diseño oficial
    private static final Color HEADER_BG = new Color(31, 58, 96); // Azul marino elegante
    private static final Color SUBHEADER_BG = new Color(217, 226, 236); // Azul pastel claro
    private static final Color TOTAL_BG = new Color(226, 232, 240); // Gris azulado suave
    private static final Color BORDER_COLOR = new Color(203, 213, 225); // Gris suave de bordes
    private static final Color TEXT_MAIN = new Color(30, 41, 59); // Texto oscuro legible

    public void exportarRecibosPdf(PlanillaCabecera planilla, OutputStream out) {
        // Documento A4 vertical con márgenes adecuados
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            if (planilla.getDetalles() == null || planilla.getDetalles().isEmpty()) {
                Paragraph p = new Paragraph("No hay empleados registrados en esta planilla.", new Font(Font.HELVETICA, 12, Font.BOLD));
                p.setAlignment(Element.ALIGN_CENTER);
                document.add(p);
                document.close();
                return;
            }

            int totalEmpleados = planilla.getDetalles().size();
            int index = 0;

            for (PlanillaDetalle detalle : planilla.getDetalles()) {
                if (index > 0) {
                    document.newPage();
                }
                agregarReciboEmpleado(document, planilla, detalle);
                index++;
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de recibos: " + e.getMessage(), e);
        }
    }

    public void exportarRecibosConsolidadosPdf(List<PlanillaCabecera> planillas, OutputStream out) {
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            if (planillas == null || planillas.isEmpty()) {
                Paragraph p = new Paragraph("No se han seleccionado planillas.", new Font(Font.HELVETICA, 12, Font.BOLD));
                p.setAlignment(Element.ALIGN_CENTER);
                document.add(p);
                document.close();
                return;
            }

            // Determinar período consolidado (fechaDesde mínima y fechaHasta máxima)
            LocalDate minFechaDesde = null;
            LocalDate maxFechaHasta = null;
            LocalDate maxFechaPago = null;

            for (PlanillaCabecera p : planillas) {
                if (p.getFechaDesde() != null) {
                    if (minFechaDesde == null || p.getFechaDesde().isBefore(minFechaDesde)) {
                        minFechaDesde = p.getFechaDesde();
                    }
                }
                if (p.getFechaHasta() != null) {
                    if (maxFechaHasta == null || p.getFechaHasta().isAfter(maxFechaHasta)) {
                        maxFechaHasta = p.getFechaHasta();
                    }
                }
                if (p.getFechaPago() != null) {
                    if (maxFechaPago == null || p.getFechaPago().isAfter(maxFechaPago)) {
                        maxFechaPago = p.getFechaPago();
                    }
                }
            }

            PlanillaCabecera consCabecera = new PlanillaCabecera();
            consCabecera.setNombre("CONSOLIDADO DE PLANILLAS");
            consCabecera.setFechaDesde(minFechaDesde);
            consCabecera.setFechaHasta(maxFechaHasta);
            consCabecera.setFechaPago(maxFechaPago != null ? maxFechaPago : maxFechaHasta);

            // Agrupar y sumarizar detalles por empleado (cédula)
            Map<String, PlanillaDetalle> consMap = new LinkedHashMap<>();
            Map<String, Double> overtimeMontoMap = new HashMap<>();
            Map<String, Double> baseSalaryMontoMap = new HashMap<>();

            for (PlanillaCabecera p : planillas) {
                if (p.getDetalles() == null) continue;
                for (PlanillaDetalle d : p.getDetalles()) {
                    String cedula = d.getCedula() != null ? d.getCedula().trim() : "";
                    if (cedula.isEmpty()) continue;

                    double he = d.getTotalHorasExtras() != null ? d.getTotalHorasExtras() : 0.0;
                    double impHe = d.getImporteHoraExtra() != null ? d.getImporteHoraExtra() : 0.0;
                    double montoHe = he * impHe;
                    overtimeMontoMap.put(cedula, overtimeMontoMap.getOrDefault(cedula, 0.0) + montoHe);

                    double dJornal = d.getJornal() != null ? d.getJornal() : 0.0;
                    double dDias = d.getTotalDias() != null ? d.getTotalDias() : 0.0;
                    double dImpDom = d.getImporteDomingo() != null ? d.getImporteDomingo() : 0.0;
                    double dBaseSal = (dJornal > 0 && dDias > 0)
                            ? Math.round((dDias * dJornal) + dImpDom)
                            : (d.getGrossPay() != null ? Math.max(0.0, Math.round(d.getGrossPay()) - Math.round(montoHe)) : 0.0);
                    baseSalaryMontoMap.put(cedula, baseSalaryMontoMap.getOrDefault(cedula, 0.0) + dBaseSal);

                    PlanillaDetalle cons = consMap.get(cedula);
                    if (cons == null) {
                        cons = new PlanillaDetalle();
                        cons.setCedula(cedula);
                        cons.setNombre(d.getNombre());
                        cons.setMetodoPago(d.getMetodoPago() != null ? d.getMetodoPago() : "BANCO");
                        cons.setJornal(d.getJornal());
                        cons.setImporteDomingo(dImpDom);
                        cons.setTotalDias(d.getTotalDias() != null ? d.getTotalDias() : 0.0);
                        cons.setCantidadHoraSem(d.getCantidadHoraSem() != null ? d.getCantidadHoraSem() : 0.0);
                        cons.setTotalHorasExtras(he);
                        cons.setGrossPay(d.getGrossPay() != null ? d.getGrossPay() : 0.0);
                        cons.setColaboracion(d.getColaboracion() != null ? d.getColaboracion() : 0.0);
                        cons.setAnticipo(d.getAnticipo() != null ? d.getAnticipo() : 0.0);
                        cons.setDescManual(d.getDescManual() != null ? d.getDescManual() : 0.0);
                        cons.setDescuentos(d.getDescuentos() != null ? d.getDescuentos() : 0.0);
                        cons.setIps(d.getIps() != null ? d.getIps() : 0.0);
                        cons.setNetoTarjeta(d.getNetoTarjeta() != null ? d.getNetoTarjeta() : 0.0);
                        cons.setNetoTesoreria(d.getNetoTesoreria() != null ? d.getNetoTesoreria() : 0.0);
                        consMap.put(cedula, cons);
                    } else {
                        // Sumar días y horas
                        cons.setTotalDias((cons.getTotalDias() != null ? cons.getTotalDias() : 0.0) + (d.getTotalDias() != null ? d.getTotalDias() : 0.0));
                        cons.setCantidadHoraSem((cons.getCantidadHoraSem() != null ? cons.getCantidadHoraSem() : 0.0) + (d.getCantidadHoraSem() != null ? d.getCantidadHoraSem() : 0.0));
                        cons.setTotalHorasExtras((cons.getTotalHorasExtras() != null ? cons.getTotalHorasExtras() : 0.0) + he);
                        cons.setImporteDomingo((cons.getImporteDomingo() != null ? cons.getImporteDomingo() : 0.0) + dImpDom);
                        if (d.getJornal() != null && d.getJornal() > 0) {
                            cons.setJornal(d.getJornal());
                        }
                        
                        // Sumar montos de ingresos y descuentos
                        cons.setGrossPay((cons.getGrossPay() != null ? cons.getGrossPay() : 0.0) + (d.getGrossPay() != null ? d.getGrossPay() : 0.0));
                        cons.setColaboracion((cons.getColaboracion() != null ? cons.getColaboracion() : 0.0) + (d.getColaboracion() != null ? d.getColaboracion() : 0.0));
                        cons.setAnticipo((cons.getAnticipo() != null ? cons.getAnticipo() : 0.0) + (d.getAnticipo() != null ? d.getAnticipo() : 0.0));
                        cons.setDescManual((cons.getDescManual() != null ? cons.getDescManual() : 0.0) + (d.getDescManual() != null ? d.getDescManual() : 0.0));
                        cons.setDescuentos((cons.getDescuentos() != null ? cons.getDescuentos() : 0.0) + (d.getDescuentos() != null ? d.getDescuentos() : 0.0));
                        cons.setIps((cons.getIps() != null ? cons.getIps() : 0.0) + (d.getIps() != null ? d.getIps() : 0.0));
                        cons.setNetoTarjeta((cons.getNetoTarjeta() != null ? cons.getNetoTarjeta() : 0.0) + (d.getNetoTarjeta() != null ? d.getNetoTarjeta() : 0.0));
                        cons.setNetoTesoreria((cons.getNetoTesoreria() != null ? cons.getNetoTesoreria() : 0.0) + (d.getNetoTesoreria() != null ? d.getNetoTesoreria() : 0.0));
                    }
                }
            }

            if (consMap.isEmpty()) {
                Paragraph p = new Paragraph("No hay empleados registrados en las planillas seleccionadas.", new Font(Font.HELVETICA, 12, Font.BOLD));
                p.setAlignment(Element.ALIGN_CENTER);
                document.add(p);
                document.close();
                return;
            }

            // Calcular importeHoraExtra ponderado y jornal ponderado exactos
            for (Map.Entry<String, PlanillaDetalle> entry : consMap.entrySet()) {
                String ced = entry.getKey();
                PlanillaDetalle cons = entry.getValue();
                double totalHe = cons.getTotalHorasExtras() != null ? cons.getTotalHorasExtras() : 0.0;
                double totalHeMonto = overtimeMontoMap.getOrDefault(ced, 0.0);
                if (totalHe > 0) {
                    cons.setImporteHoraExtra(totalHeMonto / totalHe);
                } else {
                    cons.setImporteHoraExtra(0.0);
                }

                double totalDias = cons.getTotalDias() != null ? cons.getTotalDias() : 0.0;
                double totalBase = baseSalaryMontoMap.getOrDefault(ced, 0.0);
                double totalDom = cons.getImporteDomingo() != null ? cons.getImporteDomingo() : 0.0;
                if (totalDias > 0 && totalBase > 0) {
                    cons.setJornal((totalBase - totalDom) / totalDias);
                }
            }

            // Ordenar alfabéticamente por nombre de empleado
            List<PlanillaDetalle> sortedList = new ArrayList<>(consMap.values());
            sortedList.sort((d1, d2) -> {
                String n1 = d1.getNombre() != null ? d1.getNombre().toUpperCase() : "";
                String n2 = d2.getNombre() != null ? d2.getNombre().toUpperCase() : "";
                return n1.compareTo(n2);
            });

            int index = 0;
            for (PlanillaDetalle detalle : sortedList) {
                if (index > 0) {
                    document.newPage();
                }
                agregarReciboEmpleado(document, consCabecera, detalle);
                index++;
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de recibos consolidados: " + e.getMessage(), e);
        }
    }

    private void agregarReciboEmpleado(Document document, PlanillaCabecera planilla, PlanillaDetalle d) throws Exception {
        // Fuentes tipográficas
        Font fontGovTitle = new Font(Font.HELVETICA, 7, Font.BOLD, new Color(51, 65, 85));
        Font fontGovSub = new Font(Font.HELVETICA, 6, Font.NORMAL, new Color(71, 85, 105));
        Font fontBoxTitle = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(15, 23, 42));
        Font fontCompany = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(15, 23, 42));
        
        Font fontMetaLabel = new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_MAIN);
        Font fontMetaVal = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MAIN);
        
        Font fontColHeader = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        Font fontSectionHeader = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(30, 58, 138));
        Font fontRowBold = new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_MAIN);
        Font fontRowNormal = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MAIN);

        // ─────────────────────────────────────────────────────────────────────────
        // 1. ENCABEZADO (Logo Ministerio + Título en Recuadro + Logo Astillero)
        // ─────────────────────────────────────────────────────────────────────────
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3.4f, 3.2f, 3.4f});
        headerTable.setSpacingAfter(10);

        // Columna Izquierda: Logo Ministerio de Trabajo
        PdfPCell govCell = new PdfPCell();
        govCell.setBorder(Rectangle.NO_BORDER);
        govCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        govCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        try {
            java.io.InputStream isMin = getClass().getResourceAsStream("/static/images/ministerio_logo.jpg");
            if (isMin != null) {
                byte[] bytes = isMin.readAllBytes();
                Image minImg = Image.getInstance(bytes);
                minImg.scaleToFit(160, 42);
                minImg.setAlignment(Element.ALIGN_LEFT);
                govCell.addElement(minImg);
            } else {
                Paragraph govP = new Paragraph();
                govP.add(new Chunk("MINISTERIO DE\n", fontGovSub));
                govP.add(new Chunk("TRABAJO, EMPLEO\n", fontGovTitle));
                govP.add(new Chunk("Y SEGURIDAD SOCIAL\n", fontGovTitle));
                govP.add(new Chunk("PARAGUÁI MBA'APO, JEPOROMOMBA'APO\nHA TETÃYGUA JEIKOPORÃ MOTENONDEHA", fontGovSub));
                govCell.addElement(govP);
            }
        } catch (Exception ex) {
            Paragraph govP = new Paragraph();
            govP.add(new Chunk("MINISTERIO DE\n", fontGovSub));
            govP.add(new Chunk("TRABAJO, EMPLEO\n", fontGovTitle));
            govP.add(new Chunk("Y SEGURIDAD SOCIAL\n", fontGovTitle));
            govP.add(new Chunk("PARAGUÁI MBA'APO, JEPOROMOMBA'APO\nHA TETÃYGUA JEIKOPORÃ MOTENONDEHA", fontGovSub));
            govCell.addElement(govP);
        }
        headerTable.addCell(govCell);

        // Columna Central: Recuadro LIQUIDACION DE SALARIOS
        PdfPCell titleBoxCell = new PdfPCell();
        titleBoxCell.setBorder(Rectangle.BOX);
        titleBoxCell.setBorderWidth(1.2f);
        titleBoxCell.setBorderColor(new Color(71, 85, 105));
        titleBoxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleBoxCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleBoxCell.setPadding(8);
        titleBoxCell.setBackgroundColor(new Color(248, 250, 252));
        
        Paragraph titleP = new Paragraph("LIQUIDACION DE\nSALARIOS", fontBoxTitle);
        titleP.setAlignment(Element.ALIGN_CENTER);
        titleBoxCell.addElement(titleP);
        headerTable.addCell(titleBoxCell);

        // Columna Derecha: ASTILLERO RIVEIRO S.A Logo
        PdfPCell compCell = new PdfPCell();
        compCell.setBorder(Rectangle.NO_BORDER);
        compCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        compCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        try {
            java.io.InputStream isAst = getClass().getResourceAsStream("/static/images/astillero_logo.jpg");
            if (isAst != null) {
                byte[] bytes = isAst.readAllBytes();
                Image astImg = Image.getInstance(bytes);
                astImg.scaleToFit(140, 42);
                astImg.setAlignment(Element.ALIGN_RIGHT);
                compCell.addElement(astImg);
            } else {
                Paragraph compP = new Paragraph();
                compP.setAlignment(Element.ALIGN_RIGHT);
                compP.add(new Chunk("ASTILLERO\nRIVEIRO\nS.A", fontCompany));
                compCell.addElement(compP);
            }
        } catch (Exception ex) {
            Paragraph compP = new Paragraph();
            compP.setAlignment(Element.ALIGN_RIGHT);
            compP.add(new Chunk("ASTILLERO\nRIVEIRO\nS.A", fontCompany));
            compCell.addElement(compP);
        }
        headerTable.addCell(compCell);

        document.add(headerTable);

        // ─────────────────────────────────────────────────────────────────────────
        // 2. DATOS GENERALES DEL EMPLEADOR Y TRABAJADOR
        // ─────────────────────────────────────────────────────────────────────────
        PdfPTable metaTable = new PdfPTable(4);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{2.5f, 3.5f, 1.8f, 2.2f});
        metaTable.setSpacingAfter(10);

        String desdeStr = planilla.getFechaDesde() != null ? planilla.getFechaDesde().format(DATE_FORMATTER) : "-";
        String hastaStr = planilla.getFechaHasta() != null ? planilla.getFechaHasta().format(DATE_FORMATTER) : "-";
        String fechaPagoStr = planilla.getFechaPago() != null ? planilla.getFechaPago().format(DATE_FORMATTER) : (planilla.getFechaHasta() != null ? planilla.getFechaHasta().format(DATE_FORMATTER) : (planilla.getFechaCreacion() != null ? planilla.getFechaCreacion().format(DATE_FORMATTER) : "-"));
        
        double totalDiasVal = d.getTotalDias() != null ? d.getTotalDias() : 0.0;
        String totalDiasStr = (totalDiasVal == Math.floor(totalDiasVal)) ? String.valueOf((int) totalDiasVal) : String.valueOf(totalDiasVal);

        double totalHorasVal = d.getCantidadHoraSem() != null ? d.getCantidadHoraSem() : 0.0;
        String totalHorasStr = (totalHorasVal == Math.floor(totalHorasVal)) ? String.valueOf((int) totalHorasVal) : String.valueOf(totalHorasVal);

        // Fila 1: Empleador y RUC
        addMetaRow(metaTable, "EMPLEADOR:", "ASTILLERO RIVEIRO SA", "RUC Nº:", "80067561-4", fontMetaLabel, fontMetaVal);

        // Fila 2: Nombre Trabajador y Cédula
        addMetaRow(metaTable, "NOMBRE Y APELLIDO:", d.getNombre() != null ? d.getNombre() : "-", "C.I. Nº:", d.getCedula() != null ? d.getCedula() : "-", fontMetaLabel, fontMetaVal);

        // Fila 3: Período y Forma de Pago
        addMetaRow(metaTable, "PERÍODO DE PAGO:", "DEL: " + desdeStr + "  AL: " + hastaStr, "FORMA DE PAGO:", d.getMetodoPago() != null ? d.getMetodoPago() : "BANCO", fontMetaLabel, fontMetaVal);

        // Fila 4: Fecha de Pago
        addMetaRow(metaTable, "FECHA DE PAGO:", fechaPagoStr, "", "", fontMetaLabel, fontMetaVal);

        // Fila 5: Días y Horas Trabajadas
        addMetaRow(metaTable, "DÍAS TRABAJADOS:", totalDiasStr, "HORAS TRABAJADAS:", totalHorasStr, fontMetaLabel, fontMetaVal);

        document.add(metaTable);

        // ─────────────────────────────────────────────────────────────────────────
        // 3. CÁLCULOS FINANCIEROS (INGRESOS / EGRESOS / NETO)
        // ─────────────────────────────────────────────────────────────────────────
        double totalHorasExtras = d.getTotalHorasExtras() != null ? d.getTotalHorasExtras() : 0.0;
        double importeHoraExtra = d.getImporteHoraExtra() != null ? d.getImporteHoraExtra() : 0.0;
        double horasExtrasDiurnasMonto = Math.round(totalHorasExtras * importeHoraExtra);

        double jornal = d.getJornal() != null ? d.getJornal() : 0.0;
        double impDom = d.getImporteDomingo() != null ? d.getImporteDomingo() : 0.0;
        double salarioBasico = 0.0;

        if (jornal > 0 && totalDiasVal > 0) {
            salarioBasico = Math.round((totalDiasVal * jornal) + impDom);
        } else if (d.getGrossPay() != null && d.getGrossPay() > 0) {
            salarioBasico = Math.max(0.0, Math.round(d.getGrossPay()) - horasExtrasDiurnasMonto);
        } else {
            salarioBasico = Math.round((totalDiasVal * jornal) + impDom);
        }

        double horasExtrasNocturnasMonto = 0.0;
        double gratificaciones = d.getColaboracion() != null ? Math.round(d.getColaboracion()) : 0.0;
        double complementoSalarial = d.getAnticipo() != null ? Math.round(d.getAnticipo()) : 0.0;
        double otrosIngresos = 0.0;

        double totalIngresos = salarioBasico + horasExtrasDiurnasMonto + horasExtrasNocturnasMonto + gratificaciones + complementoSalarial + otrosIngresos;

        double anticiposSalario = d.getDescManual() != null ? Math.round(d.getDescManual()) : 0.0;
        double embargos = d.getDescuentos() != null ? Math.round(d.getDescuentos()) : 0.0;
        double ips = d.getIps() != null && d.getIps() > 0 ? Math.round(d.getIps()) : Math.round(totalIngresos * 0.09);
        double otrosDescuentos = 0.0;

        double totalEgresos = anticiposSalario + embargos + ips + otrosDescuentos;
        double totalNeto = totalIngresos - totalEgresos;

        // ─────────────────────────────────────────────────────────────────────────
        // 4. TABLA PRINCIPAL DE LIQUIDACIÓN
        // ─────────────────────────────────────────────────────────────────────────
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.2f, 2.0f, 2.0f, 1.8f});
        table.setSpacingAfter(35);

        // Cabecera Principal (Azul oscuro)
        addCellHeader(table, "CONCEPTO", fontColHeader, Element.ALIGN_LEFT);
        addCellHeader(table, "INGRESOS (GS)", fontColHeader, Element.ALIGN_RIGHT);
        addCellHeader(table, "EGRESOS (GS)", fontColHeader, Element.ALIGN_RIGHT);
        addCellHeader(table, "OBSERVACIONES", fontColHeader, Element.ALIGN_CENTER);

        // ── SECCIÓN INGRESOS ───────────────────────────────────────────────
        addSectionHeader(table, "INGRESOS", fontSectionHeader);
        addDataRow(table, "SALARIO BÁSICO", formatAmount(salarioBasico), "-", "", fontRowNormal);
        addDataRow(table, "HORAS EXTRAS DIURNAS (50%)", formatAmount(horasExtrasDiurnasMonto), "-", "", fontRowNormal);
        addDataRow(table, "HORAS EXTRAS NOCTURNAS (100%)", formatAmount(horasExtrasNocturnasMonto), "-", "", fontRowNormal);
        addDataRow(table, "GRATIFICACIONES", formatAmount(gratificaciones), "-", "", fontRowNormal);
        addDataRow(table, "COMPLEMENTO SALARIAL", formatAmount(complementoSalarial), "-", "", fontRowNormal);
        addDataRow(table, "OTROS INGRESOS (DETALLAR)", formatAmount(otrosIngresos), "-", "", fontRowNormal);
        
        // Total Ingresos
        addTotalRow(table, "MONTO TOTAL DE INGRESOS", formatAmount(totalIngresos), "", "", fontRowBold);

        // ── SECCIÓN EGRESOS / DESCUENTOS ──────────────────────────────────
        addSectionHeader(table, "EGRESOS / DESCUENTOS", fontSectionHeader);
        addDataRow(table, "ANTICIPOS DE SALARIO", "-", formatAmount(anticiposSalario), "", fontRowNormal);
        addDataRow(table, "EMBARGOS JUDICIALES", "-", formatAmount(embargos), "", fontRowNormal);
        addDataRow(table, "APORTE IPS (9%)", "-", formatAmount(ips), "", fontRowNormal);
        addDataRow(table, "OTROS DESCUENTOS (DETALLAR)", "-", formatAmount(otrosDescuentos), "", fontRowNormal);
        
        // Total Egresos
        addTotalRow(table, "MONTO TOTAL DE EGRESOS", "", formatAmount(totalEgresos), "", fontRowBold);

        // ── MONTO TOTAL A COBRAR (NETO) ──────────────────────────────────
        PdfPCell netLabelCell = new PdfPCell(new Phrase("MONTO TOTAL A COBRAR (NETO)", fontRowBold));
        netLabelCell.setBackgroundColor(SUBHEADER_BG);
        netLabelCell.setPadding(6);
        netLabelCell.setBorder(Rectangle.BOX);
        netLabelCell.setBorderColor(BORDER_COLOR);
        table.addCell(netLabelCell);

        PdfPCell netValCell = new PdfPCell(new Phrase(formatAmount(totalNeto), fontRowBold));
        netValCell.setBackgroundColor(SUBHEADER_BG);
        netValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netValCell.setPadding(6);
        netValCell.setBorder(Rectangle.BOX);
        netValCell.setBorderColor(BORDER_COLOR);
        table.addCell(netValCell);

        PdfPCell emptyNetCell = new PdfPCell(new Phrase("", fontRowNormal));
        emptyNetCell.setColspan(2);
        emptyNetCell.setBackgroundColor(SUBHEADER_BG);
        emptyNetCell.setBorder(Rectangle.BOX);
        emptyNetCell.setBorderColor(BORDER_COLOR);
        table.addCell(emptyNetCell);

        document.add(table);

        // ─────────────────────────────────────────────────────────────────────────
        // 5. FIRMAS AL PIE DE PÁGINA
        // ─────────────────────────────────────────────────────────────────────────
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(90);
        signTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        signTable.setWidths(new float[]{1f, 1f});

        PdfPCell empSignCell = new PdfPCell();
        empSignCell.setBorder(Rectangle.NO_BORDER);
        empSignCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph empSignP = new Paragraph();
        empSignP.setAlignment(Element.ALIGN_CENTER);
        empSignP.add(new Chunk("___________________________________\n", fontRowNormal));
        empSignP.add(new Chunk("FIRMA DEL EMPLEADOR", fontRowBold));
        empSignCell.addElement(empSignP);
        signTable.addCell(empSignCell);

        PdfPCell workerSignCell = new PdfPCell();
        workerSignCell.setBorder(Rectangle.NO_BORDER);
        workerSignCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph workerSignP = new Paragraph();
        workerSignP.setAlignment(Element.ALIGN_CENTER);
        workerSignP.add(new Chunk("___________________________________\n", fontRowNormal));
        workerSignP.add(new Chunk("FIRMA DEL EMPLEADO", fontRowBold));
        workerSignCell.addElement(workerSignP);
        signTable.addCell(workerSignCell);

        document.add(signTable);
    }

    private void addMetaRow(PdfPTable table, String lbl1, String val1, String lbl2, String val2, Font fontLabel, Font fontVal) {
        PdfPCell c1 = new PdfPCell(new Phrase(lbl1, fontLabel));
        c1.setBorder(Rectangle.BOX);
        c1.setBorderColor(BORDER_COLOR);
        c1.setPadding(4);
        c1.setBackgroundColor(new Color(248, 250, 252));
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val1, fontVal));
        c2.setBorder(Rectangle.BOX);
        c2.setBorderColor(BORDER_COLOR);
        c2.setPadding(4);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(lbl2, fontLabel));
        c3.setBorder(Rectangle.BOX);
        c3.setBorderColor(BORDER_COLOR);
        c3.setPadding(4);
        c3.setBackgroundColor(new Color(248, 250, 252));
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(val2, fontVal));
        c4.setBorder(Rectangle.BOX);
        c4.setBorderColor(BORDER_COLOR);
        c4.setPadding(4);
        table.addCell(c4);
    }

    private void addCellHeader(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addSectionHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(4);
        cell.setBackgroundColor(SUBHEADER_BG);
        cell.setPadding(4);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addDataRow(PdfPTable table, String concept, String ing, String eg, String obs, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(concept, font));
        c1.setPadding(4);
        c1.setBorder(Rectangle.BOX);
        c1.setBorderColor(BORDER_COLOR);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(ing, font));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(4);
        c2.setBorder(Rectangle.BOX);
        c2.setBorderColor(BORDER_COLOR);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(eg, font));
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setPadding(4);
        c3.setBorder(Rectangle.BOX);
        c3.setBorderColor(BORDER_COLOR);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(obs, font));
        c4.setPadding(4);
        c4.setBorder(Rectangle.BOX);
        c4.setBorderColor(BORDER_COLOR);
        table.addCell(c4);
    }

    private void addTotalRow(PdfPTable table, String concept, String ing, String eg, String obs, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(concept, font));
        c1.setBackgroundColor(TOTAL_BG);
        c1.setPadding(5);
        c1.setBorder(Rectangle.BOX);
        c1.setBorderColor(BORDER_COLOR);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(ing, font));
        c2.setBackgroundColor(TOTAL_BG);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(5);
        c2.setBorder(Rectangle.BOX);
        c2.setBorderColor(BORDER_COLOR);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(eg, font));
        c3.setBackgroundColor(TOTAL_BG);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setPadding(5);
        c3.setBorder(Rectangle.BOX);
        c3.setBorderColor(BORDER_COLOR);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(obs, font));
        c4.setBackgroundColor(TOTAL_BG);
        c4.setPadding(5);
        c4.setBorder(Rectangle.BOX);
        c4.setBorderColor(BORDER_COLOR);
        table.addCell(c4);
    }

    private String formatAmount(double val) {
        return NUMBER_FORMAT.format(Math.round(val));
    }
}
