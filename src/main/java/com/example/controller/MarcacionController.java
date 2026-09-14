package com.example.controller;

import com.example.domain.Marcacion;
import com.example.service.MarcacionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.service.MarcacionExportService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
public class MarcacionController {

    private final MarcacionService service;
    private final MarcacionExportService exportService;

    public MarcacionController(MarcacionService service, MarcacionExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    // ── Login ──────────────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ── Dashboard ──────────────────────────────────────────────────────────────
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("stats", service.getEstadisticas());
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }

    // ── Listado con filtros ────────────────────────────────────────────────────
    @GetMapping("/marcaciones")
    public String list(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "puntero", required = false) String puntero,
            Model model) {

        List<Marcacion> marcaciones = service.search(query, startDate, endDate, puntero);
        
        String punteroNombre = null;
        if (puntero != null && !puntero.isEmpty()) {
            try {
                punteroNombre = com.example.domain.PunteroEnum.valueOf(puntero).getDisplayName();
            } catch (IllegalArgumentException e) {
                // Si el key no es válido, ignoramos
            }
        }

        model.addAttribute("marcaciones", marcaciones);
        model.addAttribute("query", query);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("puntero", puntero);
        model.addAttribute("punteroNombre", punteroNombre);
        model.addAttribute("activePage", "list");
        return "list";
    }

    // ── Carga de archivo ──────────────────────────────────────────────────────
    @GetMapping("/marcaciones/upload")
    public String uploadForm(Model model) {
        model.addAttribute("activePage", "upload");
        return "upload";
    }

    @PostMapping("/marcaciones/upload")
    public String processUpload(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor seleccione un archivo para subir.");
            return "redirect:/marcaciones/upload";
        }

        try {
            int count = service.cargarMarcacionesDesdeArchivo(file.getInputStream());
            redirectAttributes.addFlashAttribute("success", "Se importaron correctamente " + count + " registros de marcaciones.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error procesando el archivo: " + e.getMessage());
            return "redirect:/marcaciones/upload";
        }

        return "redirect:/marcaciones";
    }

    // ── Formulario manual (solo ADMIN) ────────────────────────────────────────
    @GetMapping("/marcaciones/nuevo")
    public String newForm(Model model) {
        Marcacion m = new Marcacion();
        m.setFecha(LocalDate.now());
        model.addAttribute("marcacion", m);
        model.addAttribute("title", "Nueva Marcación Manual");
        model.addAttribute("activePage", "form");
        return "form";
    }

    @GetMapping("/marcaciones/editar/{id}")
    public String editForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return service.findById(id)
                .map(m -> {
                    model.addAttribute("marcacion", m);
                    model.addAttribute("title", "Editar Marcación");
                    model.addAttribute("activePage", "form");
                    return "form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Registro no encontrado.");
                    return "redirect:/marcaciones";
                });
    }

    @PostMapping("/marcaciones/guardar")
    public String save(@ModelAttribute("marcacion") Marcacion marcacion, RedirectAttributes redirectAttributes) {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                marcacion.setUsuarioModificacion(auth.getName());
            }
            service.save(marcacion);
            redirectAttributes.addFlashAttribute("success", "Registro guardado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el registro: " + e.getMessage());
        }
        return "redirect:/marcaciones";
    }

    // ── Eliminación (solo ADMIN) ───────────────────────────────────────────────
    @GetMapping("/marcaciones/eliminar/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Registro eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/marcaciones";
    }

    @GetMapping("/marcaciones/limpiar")
    public String clearAll(RedirectAttributes redirectAttributes) {
        try {
            service.deleteAll();
            redirectAttributes.addFlashAttribute("success", "Se eliminaron todos los registros de marcaciones.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al limpiar base de datos: " + e.getMessage());
        }
        return "redirect:/marcaciones";
    }

    // ── Descarga de plantilla de ejemplo ─────────────────────────────────────
    @GetMapping("/marcaciones/ejemplo")
    @ResponseBody
    public ResponseEntity<byte[]> downloadSampleFile() {
        String sampleData = "# Formato: Cedula, Nombre, Fecha, Entrada, Salida Almuerzo, Entrada Almuerzo, Salida Laboral\n" +
                "124578,Juan Perez,2026-07-08,07:00,12:00,13:00,17:00\n" +
                "785412,Maria Gomez,2026-07-08,07:15,12:00,13:00,17:00\n" +
                "365214,Carlos Lopez,2026-07-08,06:55,11:50,13:05,17:00\n" +
                "985214,Ana Martinez,2026-07-08,07:00,12:00,13:15,17:20\n" +
                "456123,Pedro Ruiz,2026-07-08,08:30,-,-,-\n" +
                "852963,Laura Benitez,2026-07-09,06:58,12:00,13:00,17:00\n";

        byte[] bytes = sampleData.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=marcaciones_ejemplo.pem")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
     }

    // ── Exportar a Excel ──────────────────────────────────────────────────────
    @GetMapping("/marcaciones/export/excel")
    public void exportToExcel(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "puntero", required = false) String puntero,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Reporte_Marcaciones_" + java.time.LocalDate.now() + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Marcacion> marcaciones = service.search(query, startDate, endDate, puntero);
        exportService.exportToExcel(marcaciones, response.getOutputStream());
    }

    // ── Exportar a PDF ────────────────────────────────────────────────────────
    @GetMapping("/marcaciones/export/pdf")
    public void exportToPdf(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "puntero", required = false) String puntero,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Reporte_Marcaciones_" + java.time.LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);

        List<Marcacion> marcaciones = service.search(query, startDate, endDate, puntero);
        exportService.exportToPdf(marcaciones, response.getOutputStream());
    }
}
