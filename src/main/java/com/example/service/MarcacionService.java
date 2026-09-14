package com.example.service;

import com.example.domain.Marcacion;
import com.example.repository.MarcacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class MarcacionService {

    private static final Logger log = LoggerFactory.getLogger(MarcacionService.class);

    private final MarcacionRepository repository;

    // Horarios estándar configurados
    private static final LocalTime HORA_ENTRADA_ST = LocalTime.of(7, 0);
    private static final LocalTime HORA_SALIDA_ALMUERZO_ST = LocalTime.of(12, 0);
    private static final LocalTime HORA_ENTRADA_ALMUERZO_ST = LocalTime.of(13, 0);
    private static final LocalTime HORA_SALIDA_LABORAL_ST = LocalTime.of(17, 0);

    // Constructor manually created for dependency injection (no Lombok dependency)
    public MarcacionService(MarcacionRepository repository) {
        this.repository = repository;
    }

    public List<Marcacion> search(String query, LocalDate startDate, LocalDate endDate, String puntero) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1900, 1, 1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.of(2100, 12, 31);
        List<Marcacion> results = repository.searchMarcaciones(query, start, end);

        if (puntero != null && !puntero.isEmpty()) {
            try {
                com.example.domain.PunteroEnum p = com.example.domain.PunteroEnum.valueOf(puntero);
                List<String> team = p.getCedulas();
                results = results.stream()
                        .filter(m -> {
                            String norm = com.example.domain.PunteroEnum.normalizeCedula(m.getCedula());
                            return team.contains(norm);
                        })
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Si el key no es válido, no filtramos por puntero
            }
        }
        return results;
    }

    public Optional<Marcacion> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Marcacion save(Marcacion marcacion) {
        calcularMetricas(marcacion);
        return repository.save(marcacion);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAllInBatch();
    }

    public void calcularMetricas(Marcacion m) {
        // Inicializar todas a cero por defecto
        m.setMinutosAtrasoEntrada(0);
        m.setMinutosAtrasoAlmuerzo(0);
        m.setMinutosSalidaTempranaAlmuerzo(0);
        m.setMinutosSalidaTempranaLaboral(0);
        m.setHorasTrabajadas(0.0);
        m.setHorasExtras(0.0);

        if (m.getFecha() == null) {
            m.setEstado("INCOMPLETO");
            return;
        }

        // Obtener el día de la semana
        java.time.DayOfWeek day = m.getFecha().getDayOfWeek();
        boolean isSaturday = (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY);
        boolean isFriday = (day == java.time.DayOfWeek.FRIDAY);

        // Horarios Límite
        LocalTime horaEntradaLimite = LocalTime.of(7, 0);
        LocalTime horaSalidaAlmuerzoLimite = LocalTime.of(12, 0);
        LocalTime horaEntradaAlmuerzoLimite = LocalTime.of(13, 0);

        LocalTime horaSalidaLaboralLimite;
        if (isSaturday) {
            horaSalidaLaboralLimite = LocalTime.of(11, 30);
        } else if (isFriday) {
            horaSalidaLaboralLimite = LocalTime.of(16, 0);
        } else {
            horaSalidaLaboralLimite = LocalTime.of(16, 30);
        }

        // 1. Atraso Entrada (tiempo después de la hora límite de entrada 07:00)
        if (m.getHoraEntrada() != null && m.getHoraEntrada().isAfter(horaEntradaLimite)) {
            long min = Duration.between(horaEntradaLimite, m.getHoraEntrada()).toMinutes();
            m.setMinutosAtrasoEntrada((int) min);
        }

        // 2. Salida Temprana Almuerzo (salida al almuerzo antes de las 12:00)
        if (!isSaturday) {
            if (m.getHoraSalidaAlmuerzo() != null && m.getHoraSalidaAlmuerzo().isBefore(horaSalidaAlmuerzoLimite)) {
                long min = Duration.between(m.getHoraSalidaAlmuerzo(), horaSalidaAlmuerzoLimite).toMinutes();
                m.setMinutosSalidaTempranaAlmuerzo((int) min);
            }

            // Atraso Almuerzo (entrada después de las 13:00)
            if (m.getHoraEntradaAlmuerzo() != null && m.getHoraEntradaAlmuerzo().isAfter(horaEntradaAlmuerzoLimite)) {
                long min = Duration.between(horaEntradaAlmuerzoLimite, m.getHoraEntradaAlmuerzo()).toMinutes();
                m.setMinutosAtrasoAlmuerzo((int) min);
            }
        }

        // 3. Salida Temprana Laboral (salida antes del límite correspondiente)
        if (m.getHoraSalidaLaboral() != null && m.getHoraSalidaLaboral().isBefore(horaSalidaLaboralLimite)) {
            long min = Duration.between(m.getHoraSalidaLaboral(), horaSalidaLaboralLimite).toMinutes();
            m.setMinutosSalidaTempranaLaboral((int) min);
        }

        // 4. Horas Extras (tiempo después de la hora de salida correspondiente)
        if (m.getHoraSalidaLaboral() != null && m.getHoraSalidaLaboral().isAfter(horaSalidaLaboralLimite)) {
            long min = Duration.between(horaSalidaLaboralLimite, m.getHoraSalidaLaboral()).toMinutes();
            m.setHorasExtras(min / 60.0);
        }

        // 5. Horas Trabajadas (sumar los intervalos registrados de forma secuencial y restar los descansos)
        List<LocalTime> activeTimes = new ArrayList<>();
        if (m.getHoraEntrada() != null) {
            // Si marca antes de las 07:00, la entrada se toma como las 07:00
            LocalTime entradaEfectiva = m.getHoraEntrada().isBefore(horaEntradaLimite)
                    ? horaEntradaLimite
                    : m.getHoraEntrada();
            activeTimes.add(entradaEfectiva);
        }
        if (m.getHoraSalidaAlmuerzo() != null) activeTimes.add(m.getHoraSalidaAlmuerzo());
        if (m.getHoraEntradaAlmuerzo() != null) activeTimes.add(m.getHoraEntradaAlmuerzo());
        if (m.getHoraSalidaLaboral() != null) {
            // Si marca salida después del límite, la salida para horas normales se limita al tope oficial
            LocalTime salidaEfectiva = m.getHoraSalidaLaboral().isAfter(horaSalidaLaboralLimite)
                    ? horaSalidaLaboralLimite
                    : m.getHoraSalidaLaboral();
            activeTimes.add(salidaEfectiva);
        }

        Collections.sort(activeTimes);

        LocalTime break1Start = LocalTime.of(9, 0);
        LocalTime break1End = LocalTime.of(9, 30);
        LocalTime break2Start = LocalTime.of(15, 0);
        LocalTime break2End = LocalTime.of(15, 30);
        
        // Almuerzo obligatorio de 12:00 a 13:00 (Solo de Lunes a Viernes)
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(13, 0);

        long totalMinutos = 0;
        if (activeTimes.size() >= 2) {
            // Primer intervalo (Mañana o jornada completa si solo hay 2 marcas)
            LocalTime start1 = activeTimes.get(0);
            LocalTime end1 = activeTimes.get(1);
            long diff1 = Duration.between(start1, end1).toMinutes();
            long overlap1_b1 = getOverlapMinutes(start1, end1, break1Start, break1End);
            long overlap1_b2 = getOverlapMinutes(start1, end1, break2Start, break2End);
            long overlap1_lunch = (!isSaturday) ? getOverlapMinutes(start1, end1, lunchStart, lunchEnd) : 0;
            totalMinutos += Math.max(0, diff1 - overlap1_b1 - overlap1_b2 - overlap1_lunch);

            // Segundo intervalo (Tarde, si hay 4 o más marcas)
            if (activeTimes.size() >= 4) {
                LocalTime start2 = activeTimes.get(2);
                LocalTime end2 = activeTimes.get(3);
                long diff2 = Duration.between(start2, end2).toMinutes();
                long overlap2_b1 = getOverlapMinutes(start2, end2, break1Start, break1End);
                long overlap2_b2 = getOverlapMinutes(start2, end2, break2Start, break2End);
                long overlap2_lunch = (!isSaturday) ? getOverlapMinutes(start2, end2, lunchStart, lunchEnd) : 0;
                totalMinutos += Math.max(0, diff2 - overlap2_b1 - overlap2_b2 - overlap2_lunch);
            }
        }
        m.setHorasTrabajadas(Math.max(0, totalMinutos) / 60.0);

        // 6. Determinar Estado final
        // Sábado requiere 2 marcas; de Lunes a Viernes requiere 4 marcas para no ser "INCOMPLETO"
        boolean incompleto;
        if (isSaturday) {
            incompleto = (m.getHoraEntrada() == null || m.getHoraSalidaLaboral() == null);
        } else {
            incompleto = (m.getHoraEntrada() == null || m.getHoraSalidaAlmuerzo() == null ||
                          m.getHoraEntradaAlmuerzo() == null || m.getHoraSalidaLaboral() == null);
        }

        if (incompleto) {
            m.setEstado("INCOMPLETO");
        } else if (m.getMinutosAtrasoEntrada() > 0 || m.getMinutosAtrasoAlmuerzo() > 0 ||
                m.getMinutosSalidaTempranaAlmuerzo() > 0 || m.getMinutosSalidaTempranaLaboral() > 0) {
            m.setEstado("CON_ATRASO");
        } else {
            m.setEstado("NORMAL");
        }
    }

    @Transactional
    public int cargarMarcacionesDesdeArchivo(InputStream inputStream) throws Exception {
        int registrosImportados = 0;
        
        // Mapa temporal para agrupar marcaciones individuales
        // Clave: cedula_fecha
        Map<String, TempMarcacionInfo> tempMap = new LinkedHashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Ignorar líneas vacías o de comentarios
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Detectar si es el formato con columnas múltiples por espacios/tabs
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 6 && isNewFormat(tokens)) {
                    // Ignorar la cabecera del formato
                    if (tokens[0].equalsIgnoreCase("No") || tokens[2].equalsIgnoreCase("UserId")) {
                        continue;
                    }
                    try {
                        // tokens[0] = No
                        // tokens[1] = DevId
                        // tokens[2] = UserId (Cedula)
                        // tokens[3..n-4] = UName (Nombre, puede tener espacios)
                        // tokens[n-3] = Verify
                        // tokens[n-2] = Date (YYYY/MM/DD o similar)
                        // tokens[n-1] = Time (HH:mm:ss)
                        
                        int n = tokens.length;
                        String cedula = tokens[2].trim();
                        String dateStr = tokens[n - 2].trim();
                        String timeStr = tokens[n - 1].trim();

                        // Recomponer nombre de empleado (por si tiene espacios)
                        StringBuilder nameBuilder = new StringBuilder();
                        for (int i = 3; i <= n - 4; i++) {
                            if (nameBuilder.length() > 0) nameBuilder.append(" ");
                            nameBuilder.append(tokens[i]);
                        }
                        String nombre = nameBuilder.toString().trim();

                        LocalDate fecha = parseDate(dateStr);
                        LocalTime hora = parseTime(timeStr);

                        String key = cedula + "_" + fecha.toString();
                        TempMarcacionInfo info = tempMap.computeIfAbsent(key, k -> new TempMarcacionInfo(cedula, nombre, fecha));
                        
                        if (hora != null && !info.times.contains(hora)) {
                            info.times.add(hora);
                        }
                    } catch (Exception e) {
                        log.error("Error al parsear línea de formato nuevo '{}': {}", line, e.getMessage());
                    }
                } else {
                    // Formato clásico separado por comas o punto y coma
                    String[] parts = line.split("[,;\t]");
                    if (parts.length >= 3) {
                        try {
                            String cedula = parts[0].trim();
                            if (cedula.equalsIgnoreCase("cedula") || cedula.equalsIgnoreCase("cédula")) {
                                continue;
                            }
                            
                            String nombre = parts[1].trim();
                            LocalDate fecha = parseDate(parts[2].trim());
                            
                            String key = cedula + "_" + fecha.toString();
                            TempMarcacionInfo info = tempMap.computeIfAbsent(key, k -> new TempMarcacionInfo(cedula, nombre, fecha));

                            for (int i = 3; i < parts.length; i++) {
                                LocalTime hora = parseTime(parts[i]);
                                if (hora != null && !info.times.contains(hora)) {
                                    info.times.add(hora);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error al parsear línea de formato clásico '{}': {}", line, e.getMessage());
                        }
                    }
                }
            }
        }
        
        // Guardar/actualizar agrupamientos en base de datos de manera optimizada en lote
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (TempMarcacionInfo info : tempMap.values()) {
            if (minDate == null || info.fecha.isBefore(minDate)) minDate = info.fecha;
            if (maxDate == null || info.fecha.isAfter(maxDate)) maxDate = info.fecha;
        }

        // Buscar todas las marcaciones existentes para este rango de fechas de una sola vez
        Map<String, Marcacion> existingMap = new HashMap<>();
        if (minDate != null && maxDate != null) {
            List<Marcacion> existingList = repository.findByFechaBetween(minDate, maxDate);
            for (Marcacion existing : existingList) {
                existingMap.put(existing.getCedula() + "_" + existing.getFecha(), existing);
            }
        }

        List<Marcacion> toSave = new ArrayList<>();

        for (TempMarcacionInfo info : tempMap.values()) {
            String key = info.cedula + "_" + info.fecha;
            Marcacion m = existingMap.get(key);
            
            if (m != null) {
                // Si ya existe, mezclamos los horarios anteriores con los nuevos para no perder datos
                if (m.getHoraEntrada() != null && !info.times.contains(m.getHoraEntrada())) {
                    info.times.add(m.getHoraEntrada());
                }
                if (m.getHoraSalidaAlmuerzo() != null && !info.times.contains(m.getHoraSalidaAlmuerzo())) {
                    info.times.add(m.getHoraSalidaAlmuerzo());
                }
                if (m.getHoraEntradaAlmuerzo() != null && !info.times.contains(m.getHoraEntradaAlmuerzo())) {
                    info.times.add(m.getHoraEntradaAlmuerzo());
                }
                if (m.getHoraSalidaLaboral() != null && !info.times.contains(m.getHoraSalidaLaboral())) {
                    info.times.add(m.getHoraSalidaLaboral());
                }
            } else {
                m = new Marcacion();
                m.setCedula(info.cedula);
                m.setNombre(info.nombre);
                m.setFecha(info.fecha);
                m.setObservaciones("Carga automática");
            }
            
            // Ordenamos los horarios cronológicamente
            Collections.sort(info.times);
            
            // Limpiar campos antes de reasignar
            m.setHoraEntrada(null);
            m.setHoraSalidaAlmuerzo(null);
            m.setHoraEntradaAlmuerzo(null);
            m.setHoraSalidaLaboral(null);
            
            int totalMarcas = info.times.size();
            if (totalMarcas == 1) {
                m.setHoraEntrada(info.times.get(0));
            } else if (totalMarcas == 2) {
                m.setHoraEntrada(info.times.get(0));
                m.setHoraSalidaLaboral(info.times.get(1));
            } else if (totalMarcas == 3) {
                m.setHoraEntrada(info.times.get(0));
                m.setHoraSalidaAlmuerzo(info.times.get(1));
                m.setHoraSalidaLaboral(info.times.get(2));
            } else if (totalMarcas >= 4) {
                m.setHoraEntrada(info.times.get(0));
                m.setHoraSalidaAlmuerzo(info.times.get(1));
                m.setHoraEntradaAlmuerzo(info.times.get(2));
                m.setHoraSalidaLaboral(info.times.get(totalMarcas - 1)); // El último es la salida del trabajo
            }
            
            // Calcular métricas en memoria
            calcularMetricas(m);
            toSave.add(m);
            registrosImportados++;
        }
        
        // Guardar todos los registros juntos en un lote optimizado
        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
        
        return registrosImportados;
    }

    private boolean isNewFormat(String[] tokens) {
        if (tokens.length < 6) return false;
        String tok0 = tokens[0].trim();
        if (tok0.equalsIgnoreCase("No") || tok0.equalsIgnoreCase("000001")) {
            return true;
        }
        try {
            Integer.parseInt(tok0);
            String dateStr = tokens[tokens.length - 2].trim();
            return dateStr.contains("/") || dateStr.contains("-");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private LocalDate parseDate(String value) {
        value = value.trim();
        try {
            String[] parts;
            if (value.contains("/")) {
                parts = value.split("/");
            } else if (value.contains("-")) {
                parts = value.split("-");
            } else {
                return LocalDate.parse(value);
            }

            if (parts.length == 3) {
                int p0 = Integer.parseInt(parts[0]);
                int p1 = Integer.parseInt(parts[1]);
                int p2 = Integer.parseInt(parts[2]);
                if (parts[0].length() == 4) {
                    // YYYY/MM/DD o YYYY-MM-DD
                    return LocalDate.of(p0, p1, p2);
                } else {
                    // DD/MM/YYYY o DD-MM-YYYY
                    if (p2 < 100) p2 += 2000;
                    return LocalDate.of(p2, p1, p0);
                }
            }
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fecha no soportada: '" + value + "'");
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("-")) {
            return null;
        }
        value = value.trim().toLowerCase();
        try {
            boolean isPm = value.contains("pm");
            boolean isAm = value.contains("am");
            value = value.replace("am", "").replace("pm", "").trim();

            String[] parts = value.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (isPm && hour < 12) hour += 12;
            if (isAm && hour == 12) hour = 0;

            return LocalTime.of(hour, minute, second);
        } catch (Exception e) {
            throw new IllegalArgumentException("Hora no soportada: '" + value + "'");
        }
    }

    public Map<String, Object> getEstadisticas() {
        List<Marcacion> marcaciones = repository.findAll();
        Map<String, Object> stats = new HashMap<>();

        long total = marcaciones.size();
        long normales = marcaciones.stream().filter(m -> "NORMAL".equals(m.getEstado())).count();
        long atrasos = marcaciones.stream().filter(m -> "CON_ATRASO".equals(m.getEstado())).count();
        long incompletas = marcaciones.stream().filter(m -> "INCOMPLETO".equals(m.getEstado())).count();

        double promedioHoras = marcaciones.stream()
                .mapToDouble(Marcacion::getHorasTrabajadas)
                .filter(h -> h > 0.0)
                .average()
                .orElse(0.0);

        long totalMinutosAtraso = marcaciones.stream()
                .mapToLong(m -> m.getMinutosAtrasoEntrada() + m.getMinutosAtrasoAlmuerzo() +
                                m.getMinutosSalidaTempranaAlmuerzo() + m.getMinutosSalidaTempranaLaboral())
                .sum();

        stats.put("total", total);
        stats.put("normales", normales);
        stats.put("atrasos", atrasos);
        stats.put("incompletas", incompletas);
        stats.put("promedioHoras", Math.round(promedioHoras * 10.0) / 10.0);
        stats.put("totalMinutosAtraso", totalMinutosAtraso);

        return stats;
    }

    private long getOverlapMinutes(LocalTime wStart, LocalTime wEnd, LocalTime bStart, LocalTime bEnd) {
        if (wStart == null || wEnd == null || bStart == null || bEnd == null) {
            return 0;
        }
        if (wStart.isAfter(wEnd)) {
            return 0; // Intervalo de trabajo no válido
        }
        LocalTime maxStart = wStart.isAfter(bStart) ? wStart : bStart;
        LocalTime minEnd = wEnd.isBefore(bEnd) ? wEnd : bEnd;
        if (maxStart.isBefore(minEnd)) {
            return Duration.between(maxStart, minEnd).toMinutes();
        }
        return 0;
    }

    // Estructura auxiliar para guardar la información agrupada temporalmente
    private static class TempMarcacionInfo {
        String cedula;
        String nombre;
        LocalDate fecha;
        List<LocalTime> times = new ArrayList<>();
        
        TempMarcacionInfo(String cedula, String nombre, LocalDate fecha) {
            this.cedula = cedula;
            this.nombre = nombre;
            this.fecha = fecha;
        }
    }
}
