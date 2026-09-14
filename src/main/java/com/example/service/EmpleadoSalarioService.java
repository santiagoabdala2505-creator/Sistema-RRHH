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

        // 2. Agrupar horas normales y horas extras por cédula
        Map<String, Double> normalHoursMap = new HashMap<>();
        Map<String, Double> extraHoursMap = new HashMap<>();
        Map<String, String> namesMap = new HashMap<>();

        for (Marcacion m : marcaciones) {
            String cedula = m.getCedula();
            namesMap.put(cedula, m.getNombre());
            normalHoursMap.put(cedula, normalHoursMap.getOrDefault(cedula, 0.0) + (m.getHorasTrabajadas() != null ? m.getHorasTrabajadas() : 0.0));
            extraHoursMap.put(cedula, extraHoursMap.getOrDefault(cedula, 0.0) + (m.getHorasExtras() != null ? m.getHorasExtras() : 0.0));
        }

        // 3. Obtener configuraciones de salario de la BD
        List<EmpleadoSalario> configs = repository.findAll();
        Map<String, EmpleadoSalario> configMap = configs.stream()
                .collect(Collectors.toMap(EmpleadoSalario::getCedula, c -> c));

        List<EmpleadoSalarioDTO> dtos = new ArrayList<>();

        // 4. Crear DTOs para cada empleado único encontrado en las marcaciones o en las configuraciones
        Set<String> allCedulas = new HashSet<>();
        allCedulas.addAll(namesMap.keySet());
        allCedulas.addAll(configMap.keySet());

        for (String cedula : allCedulas) {
            if (cedula == null || cedula.trim().isEmpty()) {
                continue;
            }

            EmpleadoSalario config = configMap.get(cedula);
            String nombre = namesMap.get(cedula);
            if (nombre == null && config != null) {
                nombre = config.getNombre();
            }

            // Si no existe configuración en BD, creamos una automática con valores por defecto
            if (config == null) {
                config = new EmpleadoSalario();
                config.setCedula(cedula);
                config.setNombre(nombre != null ? nombre : "Empleado " + cedula);
                config = repository.save(config);
                configMap.put(cedula, config);
            }

            Double horasNormales = normalHoursMap.getOrDefault(cedula, 0.0);
            Double horasExtras = extraHoursMap.getOrDefault(cedula, 0.0);

            EmpleadoSalarioDTO dto = new EmpleadoSalarioDTO();
            dto.setCedula(cedula);
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
