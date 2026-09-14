package com.example.service;

import com.example.domain.Marcacion;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MarcacionExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ── Exportación a Excel ────────────────────────────────────────────────────
    public void exportToExcel(List<Marcacion> marcaciones, OutputStream out) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Marcaciones");

            // Estilo para la cabecera
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Estilos para las celdas de datos
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(centerStyle);

            CellStyle leftStyle = workbook.createCellStyle();
            leftStyle.setAlignment(HorizontalAlignment.LEFT);
            setBorders(leftStyle);

            CellStyle boldCenterStyle = workbook.createCellStyle();
            boldCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldCenterStyle.setFont(boldFont);
            setBorders(boldCenterStyle);

            // Fila de Título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SGN INGENIERIA - ASTILLERO RIVEIRO");
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            
            // Fila de Subtítulo
            Row subtitleRow = sheet.createRow(1);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue("Reporte General de Marcaciones de Asistencia");
            CellStyle subtitleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setFontHeightInPoints((short) 10);
            subtitleStyle.setFont(subtitleFont);
            subtitleCell.setCellStyle(subtitleStyle);
            
            // Fila vacía
            sheet.createRow(2);

            // Cabeceras de las columnas en fila 3
            String[] headers = {
                "Cédula", "Nombre", "Fecha", "Entrada", 
                "Alm. Salida", "Alm. Entrada", "Salida", 
                "Horas Normales", "Horas Extras", "Total Horas", "Obs."
            };

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos a partir de fila 4
            int rowIdx = 4;
            for (Marcacion m : marcaciones) {
                Row row = sheet.createRow(rowIdx++);

                // Cédula
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(m.getCedula());
                cell0.setCellStyle(centerStyle);

                // Nombre
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(m.getNombre());
                cell1.setCellStyle(leftStyle);

                // Fecha
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(m.getFecha() != null ? m.getFecha().format(DATE_FORMATTER) : "-");
                cell2.setCellStyle(centerStyle);

                // Entrada
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(m.getHoraEntrada() != null ? m.getHoraEntrada().format(TIME_FORMATTER) : "-");
                cell3.setCellStyle(centerStyle);

                // Alm. Salida
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(m.getHoraSalidaAlmuerzo() != null ? m.getHoraSalidaAlmuerzo().format(TIME_FORMATTER) : "-");
                cell4.setCellStyle(centerStyle);

                // Alm. Entrada
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(m.getHoraEntradaAlmuerzo() != null ? m.getHoraEntradaAlmuerzo().format(TIME_FORMATTER) : "-");
                cell5.setCellStyle(centerStyle);

                // Salida
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(m.getHoraSalidaLaboral() != null ? m.getHoraSalidaLaboral().format(TIME_FORMATTER) : "-");
                cell6.setCellStyle(centerStyle);

                // Horas Normales
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(m.getHorasTrabajadasFormateadas());
                cell7.setCellStyle(boldCenterStyle);

                // Horas Extras
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(m.getHorasExtrasFormateadas());
                cell8.setCellStyle(boldCenterStyle);

                // Total Horas
                Cell cell9 = row.createCell(9);
                cell9.setCellValue(m.getHorasTotalesFormateadas());
                cell9.setCellStyle(boldCenterStyle);

                // Obs.
                Cell cell10 = row.createCell(10);
                String obsText = m.getObservaciones() != null ? m.getObservaciones() : "-";
                if (m.getUsuarioModificacion() != null) {
                    obsText = "[" + m.getUsuarioModificacion() + "] " + obsText;
                }
                cell10.setCellValue(obsText);
                cell10.setCellStyle(leftStyle);
            }

            // Calcular totales
            long totalWorkedMin = Math.round(marcaciones.stream().mapToDouble(Marcacion::getHorasTrabajadas).sum() * 60);
            long totalOvertimeMin = Math.round(marcaciones.stream().mapToDouble(Marcacion::getHorasExtras).sum() * 60);
            long totalAllMin = totalWorkedMin + totalOvertimeMin;

            long hTrabajadas = totalWorkedMin / 60;
            long mTrabajadas = totalWorkedMin % 60;
            String formattedTotalWorked = hTrabajadas + "hs." + mTrabajadas + "min.";

            long hExtras = totalOvertimeMin / 60;
            long mExtras = totalOvertimeMin % 60;
            String formattedTotalOvertime = hExtras + "hs." + mExtras + "min.";

            long hAll = totalAllMin / 60;
            long mAll = totalAllMin % 60;
            String formattedTotalAll = hAll + "hs." + mAll + "min.";

            // Agregar fila de Total General en Excel
            Row totalRow = sheet.createRow(rowIdx++);
            
            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            org.apache.poi.ss.usermodel.Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalLabelStyle.setFont(totalFont);
            setBorders(totalLabelStyle);

            CellStyle totalValueStyle = workbook.createCellStyle();
            totalValueStyle.setAlignment(HorizontalAlignment.CENTER);
            totalValueStyle.setFont(totalFont);
            setBorders(totalValueStyle);

            // Celdas vacías previas con bordes
            for (int i = 0; i <= 10; i++) {
                Cell cell = totalRow.createCell(i);
                cell.setCellStyle(totalLabelStyle);
            }

            // Sobreescribir celda 2 para mostrar días totales trabajados
            Cell daysCell = totalRow.getCell(2);
            daysCell.setCellValue(marcaciones.size() + " días");
            daysCell.setCellStyle(totalValueStyle);

            Cell labelCell = totalRow.getCell(6);
            labelCell.setCellValue("TOTAL GENERAL:");
            labelCell.setCellStyle(totalLabelStyle);

            Cell cell7 = totalRow.getCell(7);
            cell7.setCellValue(formattedTotalWorked);
            cell7.setCellStyle(totalValueStyle);

            Cell cell8 = totalRow.getCell(8);
            cell8.setCellValue(formattedTotalOvertime);
            cell8.setCellStyle(totalValueStyle);

            Cell cell9 = totalRow.getCell(9);
            cell9.setCellValue(formattedTotalAll);
            cell9.setCellStyle(totalValueStyle);

            // Auto-ajustar tamaño de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        }
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    // ── Exportación a PDF ──────────────────────────────────────────────────────
    public void exportToPdf(List<Marcacion> marcaciones, OutputStream out) {
        // Documento en horizontal (Landscape)
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuentes
            Font fontTitle = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(15, 23, 42)); // SGN Dark Theme
            Font fontSubtitle = new Font(Font.HELVETICA, 12, Font.ITALIC, new Color(100, 116, 139));
            Font fontHeader = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font fontBody = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
            Font fontBodyBold = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);

            // Cabecera del Reporte
            Paragraph companyName = new Paragraph("SGN INGENIERIA - ASTILLERO RIVEIRO", fontTitle);
            companyName.setAlignment(Element.ALIGN_CENTER);
            document.add(companyName);

            Paragraph reportTitle = new Paragraph("Reporte General de Marcaciones de Asistencia", fontSubtitle);
            reportTitle.setAlignment(Element.ALIGN_CENTER);
            reportTitle.setSpacingAfter(20);
            document.add(reportTitle);

            // Tabla PDF
            PdfPTable table = new PdfPTable(11);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.0f, 1.2f, 0.8f, 1.0f, 1.0f, 0.8f, 1.2f, 1.2f, 1.2f, 2.2f});

            // Cabeceras de tabla
            String[] headers = {
                "Cédula", "Nombre", "Fecha", "Entrada", 
                "Alm. Salida", "Alm. Entrada", "Salida", 
                "Horas Normales", "Horas Extras", "Total Horas", "Obs."
            };

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                cell.setBackgroundColor(new Color(15, 23, 42)); // Navy Blue corporativo
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Datos de la tabla
            for (Marcacion m : marcaciones) {
                // Cédula
                table.addCell(createPdfCell(m.getCedula(), fontBody, Element.ALIGN_CENTER));
                
                // Nombre
                table.addCell(createPdfCell(m.getNombre(), fontBody, Element.ALIGN_LEFT));
                
                // Fecha
                table.addCell(createPdfCell(m.getFecha() != null ? m.getFecha().format(DATE_FORMATTER) : "-", fontBody, Element.ALIGN_CENTER));
                
                // Entrada
                table.addCell(createPdfCell(m.getHoraEntrada() != null ? m.getHoraEntrada().format(TIME_FORMATTER) : "-", fontBody, Element.ALIGN_CENTER));
                
                // Alm. Salida
                table.addCell(createPdfCell(m.getHoraSalidaAlmuerzo() != null ? m.getHoraSalidaAlmuerzo().format(TIME_FORMATTER) : "-", fontBody, Element.ALIGN_CENTER));
                
                // Alm. Entrada
                table.addCell(createPdfCell(m.getHoraEntradaAlmuerzo() != null ? m.getHoraEntradaAlmuerzo().format(TIME_FORMATTER) : "-", fontBody, Element.ALIGN_CENTER));
                
                // Salida
                table.addCell(createPdfCell(m.getHoraSalidaLaboral() != null ? m.getHoraSalidaLaboral().format(TIME_FORMATTER) : "-", fontBody, Element.ALIGN_CENTER));
                
                // Horas Normales
                table.addCell(createPdfCell(m.getHorasTrabajadasFormateadas(), fontBodyBold, Element.ALIGN_CENTER));
                
                // Horas Extras
                table.addCell(createPdfCell(m.getHorasExtrasFormateadas(), fontBodyBold, Element.ALIGN_CENTER));

                // Total Horas
                table.addCell(createPdfCell(m.getHorasTotalesFormateadas(), fontBodyBold, Element.ALIGN_CENTER));
                
                // Obs
                String obsText = m.getObservaciones() != null ? m.getObservaciones() : "-";
                if (m.getUsuarioModificacion() != null) {
                    obsText = "[" + m.getUsuarioModificacion() + "] " + obsText;
                }
                table.addCell(createPdfCell(obsText, fontBody, Element.ALIGN_LEFT));
            }

            // Calcular totales para PDF
            long totalWorkedMin = Math.round(marcaciones.stream().mapToDouble(Marcacion::getHorasTrabajadas).sum() * 60);
            long totalOvertimeMin = Math.round(marcaciones.stream().mapToDouble(Marcacion::getHorasExtras).sum() * 60);
            long totalAllMin = totalWorkedMin + totalOvertimeMin;

            long hTrabajadas = totalWorkedMin / 60;
            long mTrabajadas = totalWorkedMin % 60;
            String formattedTotalWorked = hTrabajadas + "hs." + mTrabajadas + "min.";

            long hExtras = totalOvertimeMin / 60;
            long mExtras = totalOvertimeMin % 60;
            String formattedTotalOvertime = hExtras + "hs." + mExtras + "min.";

            long hAll = totalAllMin / 60;
            long mAll = totalAllMin % 60;
            String formattedTotalAll = hAll + "hs." + mAll + "min.";

            // Fila de TOTAL GENERAL en PDF
            // 1. Celdas vacías previas para Cédula y Nombre (colspan 2)
            PdfPCell emptyPCell = new PdfPCell(new Phrase("", fontBodyBold));
            emptyPCell.setColspan(2);
            emptyPCell.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(emptyPCell);

            // 2. Celda para Fecha que muestra los días totales trabajados (colspan 1)
            PdfPCell daysPCell = new PdfPCell(new Phrase(marcaciones.size() + " días", fontBodyBold));
            daysPCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            daysPCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            daysPCell.setPadding(6);
            daysPCell.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(daysPCell);

            // 3. Celda para el rótulo "TOTAL GENERAL" que ocupa de Entrada a Salida (colspan 4)
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL GENERAL:", fontBodyBold));
            totalLabelCell.setColspan(4);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalLabelCell.setPadding(6);
            totalLabelCell.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(totalLabelCell);

            // 4. Celda de Horas Normales totales
            PdfPCell totalWorkedCell = new PdfPCell(new Phrase(formattedTotalWorked, fontBodyBold));
            totalWorkedCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalWorkedCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalWorkedCell.setPadding(6);
            totalWorkedCell.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(totalWorkedCell);

            // 5. Celda de Horas Extras totales
            PdfPCell totalOvertimeCell = new PdfPCell(new Phrase(formattedTotalOvertime, fontBodyBold));
            totalOvertimeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalOvertimeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalOvertimeCell.setPadding(6);
            totalOvertimeCell.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(totalOvertimeCell);

            // 6. Celda de Horas Totales generales
            PdfPCell totalAllCell = new PdfPCell(new Phrase(formattedTotalAll, fontBodyBold));
            totalAllCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalAllCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalAllCell.setPadding(6);
            totalAllCell.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(totalAllCell);

            // 7. Celda vacía al final para la columna Obs.
            PdfPCell emptyObsCell = new PdfPCell(new Phrase("", fontBodyBold));
            emptyObsCell.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(emptyObsCell);

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell createPdfCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        return cell;
    }
}
