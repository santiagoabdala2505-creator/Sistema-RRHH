package com.example.controller;

import com.example.domain.Legajo;
import com.example.service.LegajoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/legajos")
public class LegajoController {

    private final LegajoService service;

    @Autowired
    public LegajoController(LegajoService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("legajos", service.findAll());
        model.addAttribute("activePage", "legajos");
        return "legajos/list";
    }

    @PostMapping("/subir")
    public String upload(
            @RequestParam("nombre") String nombre,
            @RequestParam("archivo") MultipartFile archivo,
            RedirectAttributes redirectAttributes) {
        
        if (archivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor seleccione un archivo PDF.");
            return "redirect:/legajos";
        }
        
        // Validar que sea PDF
        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            // Verificar también la extensión por si el content type no se resuelve correctamente en algún navegador
            String filename = archivo.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                redirectAttributes.addFlashAttribute("error", "El archivo debe ser de formato PDF.");
                return "redirect:/legajos";
            }
        }

        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
            service.save(nombre, archivo, usuario);
            redirectAttributes.addFlashAttribute("success", "Legajo subido correctamente.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el archivo: " + e.getMessage());
        }

        return "redirect:/legajos";
    }

    @GetMapping("/descargar/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id) {
        Optional<Legajo> legajoOpt = service.findById(id);
        if (legajoOpt.isPresent()) {
            Legajo legajo = legajoOpt.get();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + legajo.getNombreArchivo() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(legajo.getArchivoDatos());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/eliminar/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Legajo eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/legajos";
    }
}
