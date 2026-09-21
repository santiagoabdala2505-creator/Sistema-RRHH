package com.example.service;

import com.example.domain.EmpleadoSalario;
import com.example.domain.Marcacion;
import com.example.dto.EmpleadoSalarioDTO;
import com.example.repository.EmpleadoSalarioRepository;
import com.example.repository.MarcacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmpleadoSalarioService {

    private final EmpleadoSalarioRepository repository;
    private final MarcacionRepository marcacionRepository;

    @Autowired
    public EmpleadoSalarioService(EmpleadoSalarioRepository repository, MarcacionRepository marcacionRepository) {
        this.repository = repository;
        this.marcacionRepository = marcacionRepository;
    }

    @Transactional
    public List<EmpleadoSalarioDTO> calculateSalaries(LocalDate startDate, LocalDate endDate) {
        // 1. Obtener todas las marcaciones en el rango de fechas
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1900, 1, 1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.of(2100, 12, 31);
        List<Marcacion> marcaciones = marcacionRepository.findByFechaBetween(start, end);

        // 2. Agrupar horas normales y horas extras por cédula normalizada
        Map<String, Double> normalHoursMap = new HashMap<>();
        Map<String, Double> extraHoursMap = new HashMap<>();
        Map<String, String> namesMap = new HashMap<>();

        for (Marcacion m : marcaciones) {
            String norm = com.example.domain.PunteroEnum.normalizeCedula(m.getCedula());
            if (norm.isEmpty()) continue;
            namesMap.put(norm, m.getNombre());
            normalHoursMap.put(norm, normalHoursMap.getOrDefault(norm, 0.0) + (m.getHorasTrabajadas() != null ? m.getHorasTrabajadas() : 0.0));
            extraHoursMap.put(norm, extraHoursMap.getOrDefault(norm, 0.0) + (m.getHorasExtras() != null ? m.getHorasExtras() : 0.0));
        }

        // 3. Obtener configuraciones de salario de la BD y mapear por cédula normalizada
        List<EmpleadoSalario> configs = repository.findAll();
        Map<String, EmpleadoSalario> configMap = new HashMap<>();
        for (EmpleadoSalario c : configs) {
            String norm = com.example.domain.PunteroEnum.normalizeCedula(c.getCedula());
            if (!norm.isEmpty() && !configMap.containsKey(norm)) {
                configMap.put(norm, c);
            }
        }

        List<EmpleadoSalarioDTO> dtos = new ArrayList<>();

        // 4. Crear DTOs para cada empleado único (por cédula normalizada)
        Set<String> allNormCedulas = new HashSet<>();
        allNormCedulas.addAll(namesMap.keySet());
        allNormCedulas.addAll(configMap.keySet());

        for (String normCedula : allNormCedulas) {
            if (normCedula == null || normCedula.trim().isEmpty()) {
                continue;
            }

            EmpleadoSalario config = configMap.get(normCedula);
            String nombre = namesMap.get(normCedula);
            if (nombre == null && config != null) {
                nombre = config.getNombre();
            }

            // Si no existe configuración en BD, creamos una automática con valores por defecto
            if (config == null) {
                config = new EmpleadoSalario();
                config.setCedula(normCedula);
                config.setNombre(nombre != null ? nombre : "Empleado " + normCedula);
                config = repository.save(config);
                configMap.put(normCedula, config);
            }

            Double horasNormales = normalHoursMap.getOrDefault(normCedula, 0.0);
            Double horasExtras = extraHoursMap.getOrDefault(normCedula, 0.0);

            EmpleadoSalarioDTO dto = new EmpleadoSalarioDTO();
            dto.setCedula(config.getCedula());
            dto.setNombre(config.getNombre());
            dto.setPagoHoraNormal(config.getPagoHoraNormal());
            dto.setPagoHoraExtra(config.getPagoHoraExtra());
            dto.setTotalHorasNormales(horasNormales);
            dto.setTotalHorasExtras(horasExtras);
            dtos.add(dto);
        }

        // Ordenar por nombre del empleado
        dtos.sort(Comparator.comparing(EmpleadoSalarioDTO::getNombre));

        return dtos;
    }

    @Transactional
    public void saveRates(String cedula, Double pagoHoraNormal, Double pagoHoraExtra) {
        Optional<EmpleadoSalario> opt = repository.findById(cedula);
        if (opt.isPresent()) {
            EmpleadoSalario config = opt.get();
            config.setPagoHoraNormal(pagoHoraNormal);
            config.setPagoHoraExtra(pagoHoraExtra);
            repository.save(config);
        }
    }

    @Transactional
    public void saveEmployee(String cedula, String nombre, Double pagoHoraNormal, Double pagoHoraExtra) {
        Optional<EmpleadoSalario> opt = repository.findById(cedula);
        if (opt.isPresent()) {
            EmpleadoSalario config = opt.get();
            config.setNombre(nombre);
            config.setPagoHoraNormal(pagoHoraNormal);
            config.setPagoHoraExtra(pagoHoraExtra);
            repository.save(config);
        }
    }

    @Transactional
    public void addEmployee(String cedula, String nombre, Double pagoHoraNormal, Double pagoHoraExtra) {
        EmpleadoSalario emp = new EmpleadoSalario(cedula, nombre, pagoHoraNormal, pagoHoraExtra);
        repository.save(emp);
    }

    @Transactional
    public void deleteEmployee(String cedula) {
        repository.deleteById(cedula);
    }
}
