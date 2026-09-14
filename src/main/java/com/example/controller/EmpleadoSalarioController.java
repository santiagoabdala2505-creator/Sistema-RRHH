package com.example.controller;

import com.example.domain.PlanillaCabecera;
import com.example.dto.EmpleadoSalarioDTO;
import com.example.service.EmpleadoSalarioService;
import com.example.service.PlanillaCabeceraService;
import com.example.service.ReciboSalarioExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/salarios")
public class EmpleadoSalarioController {

    private final EmpleadoSalarioService empleadoSalarioService;
    private final PlanillaCabeceraService planillaService;
    private final ReciboSalarioExportService reciboService;

    @Autowired
    public EmpleadoSalarioController(EmpleadoSalarioService empleadoSalarioService,
                                      PlanillaCabeceraService planillaService,
                                      ReciboSalarioExportService reciboService) {
        this.empleadoSalarioService = empleadoSalarioService;
        this.planillaService = planillaService;
        this.reciboService = reciboService;
    }

    @GetMapping
    public String mainView(Model model) {
        // 1. Obtener la lista de planillas guardadas
        List<PlanillaCabecera> planillasGuardadas = planillaService.findAll();
        
        // Ordenar planillas guardadas de más recientes a más antiguas (null-safe)
        planillasGuardadas.sort((a, b) -> {
            if (a.getFechaCreacion() == null && b.getFechaCreacion() == null) {
                if (a.getId() == null || b.getId() == null) return 0;
                return b.getId().compareTo(a.getId());
            }
            if (a.getFechaCreacion() == null) return 1;
            if (b.getFechaCreacion() == null) return -1;
            return b.getFechaCreacion().compareTo(a.getFechaCreacion());
        });
        model.addAttribute("planillas", planillasGuardadas);

        // Pre-calcular contadores de carpetas en el servidor
        long countMensualeros = planillasGuardadas.stream()
                .filter(p -> "MENSUALEROS".equalsIgnoreCase(p.getTipoCarpeta())).count();
        long countJornaleros = planillasGuardadas.size() - countMensualeros;

        Map<String, Long> monthCounts = new HashMap<>();
        String[] months = {"ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SETIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};
        for (String m : months) {
            long c = planillasGuardadas.stream()
                    .filter(p -> "MENSUALEROS".equalsIgnoreCase(p.getTipoCarpeta()) &&
                            (m.equalsIgnoreCase(p.getSubcarpetaMes()) ||
                             ("SETIEMBRE".equals(m) && "SEPTIEMBRE".equalsIgnoreCase(p.getSubcarpetaMes()))))
                    .count();
            monthCounts.put(m, c);
        }

        model.addAttribute("countMensualeros", countMensualeros);
        model.addAttribute("countJornaleros", countJornaleros);
        model.addAttribute("monthCounts", monthCounts);

        // 2. Obtener la lista de tarifas de empleados
        List<EmpleadoSalarioDTO> tarifas = empleadoSalarioService.calculateSalaries(null, null);
        model.addAttribute("tarifas", tarifas);

        // 3. Pre-cargar fechas de la semana actual por defecto para la generación
        LocalDate now = LocalDate.now();
        LocalDate lunes = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);
        model.addAttribute("fechaDesde", lunes);
        model.addAttribute("fechaHasta", domingo);

        model.addAttribute("activePage", "salarios");
        return "salarios/main";
    }

    @GetMapping("/generar")
    public String generarPlanilla(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        PlanillaCabecera planilla = planillaService.generateInMemoryPlanilla(desde, hasta);
        model.addAttribute("planilla", planilla);
        model.addAttribute("todosEmpleados", empleadoSalarioService.calculateSalaries(null, null));
        model.addAttribute("activePage", "salarios");
        model.addAttribute("esNuevo", true);
        return "salarios/spreadsheet";
    }

    @GetMapping("/editar/{id}")
    public String editarPlanilla(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<PlanillaCabecera> planillaOpt = planillaService.findById(id);
        if (planillaOpt.isPresent()) {
            model.addAttribute("planilla", planillaOpt.get());
            model.addAttribute("todosEmpleados", empleadoSalarioService.calculateSalaries(null, null));
            model.addAttribute("activePage", "salarios");
            model.addAttribute("esNuevo", false);
            return "salarios/spreadsheet";
        } else {
            redirectAttributes.addFlashAttribute("error", "Planilla no encontrada.");
            return "redirect:/salarios";
        }
    }

    @GetMapping("/recibos/pdf/{id}")
    public void exportarRecibosPdf(@PathVariable("id") Long id, HttpServletResponse response) throws Exception {
        Optional<PlanillaCabecera> planillaOpt = planillaService.findById(id);
        if (planillaOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Planilla no encontrada.");
            return;
        }

        PlanillaCabecera planilla = planillaOpt.get();
        response.setContentType("application/pdf");

        String cleanName = planilla.getNombre() != null
                ? planilla.getNombre().replaceAll("[^a-zA-Z0-9.-]", "_")
                : "Planilla_" + id;
        String headerValue = "attachment; filename=\"Recibos_" + cleanName + ".pdf\"";
        response.setHeader("Content-Disposition", headerValue);

        reciboService.exportarRecibosPdf(planilla, response.getOutputStream());
    }

    @GetMapping("/recibos/pdf/consolidado")
    public void exportarRecibosConsolidadosPdf(@RequestParam("ids") List<Long> ids, HttpServletResponse response) throws Exception {
        if (ids == null || ids.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Debe seleccionar al menos una planilla.");
            return;
        }

        List<PlanillaCabecera> planillas = new java.util.ArrayList<>();
        for (Long id : ids) {
            planillaService.findById(id).ifPresent(planillas::add);
        }

        if (planillas.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No se encontraron las planillas seleccionadas.");
            return;
        }

        response.setContentType("application/pdf");
        String headerValue = "inline; filename=\"Recibos_Consolidados_" + System.currentTimeMillis() + ".pdf\"";
        response.setHeader("Content-Disposition", headerValue);

        reciboService.exportarRecibosConsolidadosPdf(planillas, response.getOutputStream());
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<String> save(@RequestBody PlanillaCabecera cabecera) {
        try {
            planillaService.save(cabecera);
            return ResponseEntity.ok("Ok");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("error.log"),
                    "Error al guardar cabecera: " + e.getMessage() + "\n" + sw.toString()
                );
            } catch (Exception logEx) {
                logEx.printStackTrace();
            }
            return ResponseEntity.badRequest().body(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    @GetMapping("/eliminar/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            planillaService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Planilla eliminada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la planilla: " + e.getMessage());
        }
        return "redirect:/salarios";
    }

    @PostMapping("/guardar-empleado")
    @ResponseBody
    public ResponseEntity<Void> saveEmployee(
            @RequestParam("cedula") String cedula,
            @RequestParam("nombre") String nombre,
            @RequestParam("pagoHoraNormal") Double pagoHoraNormal,
            @RequestParam("pagoHoraExtra") Double pagoHoraExtra) {
        try {
            empleadoSalarioService.saveEmployee(cedula, nombre, pagoHoraNormal, pagoHoraExtra);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/agregar-empleado")
    @ResponseBody
    public ResponseEntity<Void> addEmployee(
            @RequestParam("cedula") String cedula,
            @RequestParam("nombre") String nombre,
            @RequestParam("pagoHoraNormal") Double pagoHoraNormal,
            @RequestParam("pagoHoraExtra") Double pagoHoraExtra) {
        try {
            empleadoSalarioService.addEmployee(cedula, nombre, pagoHoraNormal, pagoHoraExtra);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/eliminar-empleado")
    @ResponseBody
    public ResponseEntity<Void> deleteEmployee(@RequestParam("cedula") String cedula) {
        try {
            empleadoSalarioService.deleteEmployee(cedula);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
