package com.example.service;

import com.example.domain.EmpleadoSalario;
import com.example.domain.Marcacion;
import com.example.domain.PlanillaCabecera;
import com.example.domain.PlanillaDetalle;
import com.example.repository.EmpleadoSalarioRepository;
import com.example.repository.MarcacionRepository;
import com.example.repository.PlanillaCabeceraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanillaCabeceraService {

    private final PlanillaCabeceraRepository repository;
    private final EmpleadoSalarioRepository empleadoSalarioRepository;
    private final MarcacionRepository marcacionRepository;

    public PlanillaCabeceraService(PlanillaCabeceraRepository repository,
                                    EmpleadoSalarioRepository empleadoSalarioRepository,
                                    MarcacionRepository marcacionRepository) {
        this.repository = repository;
        this.empleadoSalarioRepository = empleadoSalarioRepository;
        this.marcacionRepository = marcacionRepository;
    }

    @PostConstruct
    @Transactional
    public void initAutoClassification() {
        autoClassifyAndPersistAll();
    }

    @Transactional
    public synchronized void autoClassifyAndPersistAll() {
        try {
            List<PlanillaCabecera> list = repository.findAll();
            boolean needsSave = false;
            for (PlanillaCabecera p : list) {
                if (classifyPlanilla(p)) {
                    needsSave = true;
                }
            }
            if (needsSave) {
                repository.saveAll(list);
                repository.flush();
            }
        } catch (Exception e) {
            System.err.println("Error auto-clasificando planillas: " + e.getMessage());
        }
    }

    public boolean classifyPlanilla(PlanillaCabecera p) {
        if (p == null) return false;
        String nom = (p.getNombre() != null) ? p.getNombre().toUpperCase().trim() : "";
        String oldTipo = p.getTipoCarpeta();
        String oldMes = p.getSubcarpetaMes();

        boolean isMensual = nom.contains("MENSUAL")
                || nom.contains("MESUAL")
                || nom.contains("TRIPULANTE")
                || nom.contains("OFICINA")
                || "MENSUALEROS".equalsIgnoreCase(p.getTipoCarpeta());

        String targetTipo;
        String targetMes;

        if (isMensual) {
            targetTipo = "MENSUALEROS";
            targetMes = detectMonthFromNameOrDate(nom, p.getFechaDesde());
        } else {
            targetTipo = "JORNALEROS";
            targetMes = null;
        }

        p.setTipoCarpeta(targetTipo);
        p.setSubcarpetaMes(targetMes);

        if (p.getFechaCreacion() == null) {
            p.setFechaCreacion(LocalDateTime.now());
        }
        if (p.getTotalNeto() == null) {
            p.setTotalNeto(0.0);
        }

        return !Objects.equals(oldTipo, targetTipo) || !Objects.equals(oldMes, targetMes);
    }

    public List<PlanillaCabecera> findAll() {
        autoClassifyAndPersistAll();
        return repository.findAll();
    }

    public Optional<PlanillaCabecera> findById(Long id) {
        Optional<PlanillaCabecera> opt = repository.findById(id);
        opt.ifPresent(this::classifyPlanilla);
        return opt;
    }

    public void normalizeFolderInfo(PlanillaCabecera p) {
        classifyPlanilla(p);
    }

    private boolean hasMonthKeyword(String nameUpper) {
        if (nameUpper == null || nameUpper.isEmpty()) return false;
        String[] months = {"ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "AGOS", "SETIEMBRE", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};
        for (String m : months) {
            if (nameUpper.contains(m)) return true;
        }
        return false;
    }

    public String detectMonthFromNameOrDate(String nameUpper, LocalDate fechaDesde) {
        if (nameUpper != null) {
            if (nameUpper.contains("ENERO")) return "ENERO";
            if (nameUpper.contains("FEBRERO")) return "FEBRERO";
            if (nameUpper.contains("MARZO")) return "MARZO";
            if (nameUpper.contains("ABRIL")) return "ABRIL";
            if (nameUpper.contains("MAYO")) return "MAYO";
            if (nameUpper.contains("JUNIO")) return "JUNIO";
            if (nameUpper.contains("JULIO")) return "JULIO";
            if (nameUpper.contains("AGOSTO") || nameUpper.contains("AGOS")) return "AGOSTO";
            if (nameUpper.contains("SETIEMBRE") || nameUpper.contains("SEPTIEMBRE")) return "SETIEMBRE";
            if (nameUpper.contains("OCTUBRE")) return "OCTUBRE";
            if (nameUpper.contains("NOVIEMBRE")) return "NOVIEMBRE";
            if (nameUpper.contains("DICIEMBRE")) return "DICIEMBRE";
        }
        
        if (fechaDesde != null) {
            int month = fechaDesde.getMonthValue();
            String[] months = {"ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SETIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};
            if (month >= 1 && month <= 12) {
                return months[month - 1];
            }
        }
        return "ENERO";
    }

    @Transactional
    public PlanillaCabecera save(PlanillaCabecera cabecera) {
        PlanillaCabecera existing = null;
        if (cabecera.getId() != null) {
            existing = repository.findById(cabecera.getId()).orElse(null);
        }

        if (existing != null) {
            // Actualizar campos cabecera
            if (cabecera.getNombre() != null && !cabecera.getNombre().trim().isEmpty()) {
                existing.setNombre(cabecera.getNombre());
            }
            if (cabecera.getFechaDesde() != null) {
                existing.setFechaDesde(cabecera.getFechaDesde());
            }
            if (cabecera.getFechaHasta() != null) {
                existing.setFechaHasta(cabecera.getFechaHasta());
            }
            if (cabecera.getFechaPago() != null) {
                existing.setFechaPago(cabecera.getFechaPago());
            }
            if (cabecera.getTipoCarpeta() != null) {
                existing.setTipoCarpeta(cabecera.getTipoCarpeta());
            }
            if (cabecera.getSubcarpetaMes() != null) {
                existing.setSubcarpetaMes(cabecera.getSubcarpetaMes());
            }
            normalizeFolderInfo(existing);

            // Eliminar detalles que ya no vienen en la lista (filtrados)
            Set<String> incomingCedulas = new HashSet<>();
            if (cabecera.getDetalles() != null) {
                for (PlanillaDetalle d : cabecera.getDetalles()) {
                    incomingCedulas.add(d.getCedula());
                }
            }
            existing.getDetalles().removeIf(d -> !incomingCedulas.contains(d.getCedula()));

            // Actualizar o insertar detalles
            Map<String, PlanillaDetalle> existingMap = new HashMap<>();
            for (PlanillaDetalle d : existing.getDetalles()) {
                existingMap.put(d.getCedula(), d);
            }

            double totalNeto = 0.0;
            if (cabecera.getDetalles() != null) {
                for (PlanillaDetalle d : cabecera.getDetalles()) {
                    PlanillaDetalle ext = existingMap.get(d.getCedula());
                    if (ext == null) {
                        ext = new PlanillaDetalle();
                        ext.setCabecera(existing);
                        ext.setCedula(d.getCedula());
                        existing.getDetalles().add(ext);
                    }
                    
                    // Copiar datos del detalle
                    ext.setNombre(d.getNombre());
                    ext.setCotizante(d.getCotizante());
                    ext.setLunes(d.getLunes());
                    ext.setMartes(d.getMartes());
                    ext.setMiercoles(d.getMiercoles());
                    ext.setJueves(d.getJueves());
                    ext.setViernes(d.getViernes());
                    ext.setSabado(d.getSabado());
                    ext.setDomingo(d.getDomingo());
                    ext.setJornal(d.getJornal());
                    ext.setHeLunes(d.getHeLunes());
                    ext.setHeMartes(d.getHeMartes());
                    ext.setHeMiercoles(d.getHeMiercoles());
                    ext.setHeJueves(d.getHeJueves());
                    ext.setHeViernes(d.getHeViernes());
                    ext.setHeSabado(d.getHeSabado());
                    ext.setCantidadHoraSem(d.getCantidadHoraSem());
                    ext.setDescManual(d.getDescManual());
                    ext.setColaboracion(d.getColaboracion());
                    ext.setMetodoPago(d.getMetodoPago());
                    ext.setDescuentos(d.getDescuentos());
                    ext.setAnticipo(d.getAnticipo());

                    // Copiar campos precalculados / editables
                    ext.setTotalDias(d.getTotalDias());
                    ext.setImporteDomingo(d.getImporteDomingo());
                    ext.setTotalHorasExtras(d.getTotalHorasExtras());
                    ext.setImporteHoraExtra(d.getImporteHoraExtra());
                    ext.setGrossPay(d.getGrossPay());
                    ext.setIps(d.getIps());
                    ext.setNetoTarjeta(d.getNetoTarjeta());
                    ext.setNetoTesoreria(d.getNetoTesoreria());

                    // Validar nulos obligatorios
                    if (ext.getCedula() == null || ext.getCedula().trim().isEmpty()) {
                        ext.setCedula("SIN_CEDULA");
                    }
                    if (ext.getNombre() == null || ext.getNombre().trim().isEmpty()) {
                        ext.setNombre("Empleado (" + ext.getCedula() + ")");
                    }

                    // Asegurar campos Double no nulos
                    checkDoubleFields(ext);

                    double netPay = ext.getNetoTarjeta() + ext.getNetoTesoreria();
                    totalNeto += netPay;
                }
            }
            existing.setTotalNeto(totalNeto);
            return repository.save(existing);
        } else {
            // Crear nueva planilla
            if (cabecera.getFechaCreacion() == null) {
                cabecera.setFechaCreacion(LocalDateTime.now());
            }
            normalizeFolderInfo(cabecera);
            if (cabecera.getDetalles() != null) {
                double totalNeto = 0.0;
                for (PlanillaDetalle d : cabecera.getDetalles()) {
                    d.setCabecera(cabecera);
                    
                    if (d.getCedula() == null || d.getCedula().trim().isEmpty()) {
                        d.setCedula("SIN_CEDULA");
                    }
                    if (d.getNombre() == null || d.getNombre().trim().isEmpty()) {
                        d.setNombre("Empleado (" + d.getCedula() + ")");
                    }

                    // Si vienen null, hacer fallback automático
                    if (d.getTotalDias() == null) {
                        double tDias = (d.getLunes() != null ? d.getLunes() : 0.0) +
                                       (d.getMartes() != null ? d.getMartes() : 0.0) +
                                       (d.getMiercoles() != null ? d.getMiercoles() : 0.0) +
                                       (d.getJueves() != null ? d.getJueves() : 0.0) +
                                       (d.getViernes() != null ? d.getViernes() : 0.0) +
                                       (d.getSabado() != null ? d.getSabado() : 0.0);
                        d.setTotalDias(tDias);
                    }
                    if (d.getImporteDomingo() == null) {
                        double jornal = d.getJornal() != null ? d.getJornal() : 120000.0;
                        double dom = d.getDomingo() != null ? d.getDomingo() : 0.0;
                        double impDomingo = (jornal / 4.25) * dom;
                        d.setImporteDomingo(impDomingo);
                    }
                    if (d.getTotalHorasExtras() == null) {
                        double tHoras = (d.getHeLunes() != null ? d.getHeLunes() : 0.0) +
                                        (d.getHeMartes() != null ? d.getHeMartes() : 0.0) +
                                        (d.getHeMiercoles() != null ? d.getHeMiercoles() : 0.0) +
                                        (d.getHeJueves() != null ? d.getHeJueves() : 0.0) +
                                        (d.getHeViernes() != null ? d.getHeViernes() : 0.0) +
                                        (d.getHeSabado() != null ? d.getHeSabado() : 0.0);
                        d.setTotalHorasExtras(tHoras);
                    }
                    if (d.getImporteHoraExtra() == null) {
                        double jornal = d.getJornal() != null ? d.getJornal() : 120000.0;
                        double impXHora = (jornal * 1.5) / 8.5;
                        d.setImporteHoraExtra(impXHora);
                    }
                    if (d.getGrossPay() == null) {
                        double jornal = d.getJornal() != null ? d.getJornal() : 120000.0;
                        double gp = (d.getTotalDias() * jornal) + d.getImporteDomingo() + (d.getTotalHorasExtras() * d.getImporteHoraExtra());
                        d.setGrossPay(gp);
                    }
                    double colab = d.getColaboracion() != null ? d.getColaboracion() : 0.0;
                    double compSal = d.getAnticipo() != null ? d.getAnticipo() : 0.0;
                    double baseImp = d.getGrossPay() + colab + compSal;
                    if (d.getIps() == null) {
                        double ipsVal = Math.round(baseImp * 0.09);
                        d.setIps(ipsVal);
                    }
                    if (d.getNetoTarjeta() == null && d.getNetoTesoreria() == null) {
                        double descManual = d.getDescManual() != null ? d.getDescManual() : 0.0;
                        double desc = d.getDescuentos() != null ? d.getDescuentos() : 0.0;
                        double netPay = baseImp - d.getIps() - descManual - desc;
                        if ("EFECTIVO".equals(d.getMetodoPago())) {
                            d.setNetoTesoreria(netPay);
                            d.setNetoTarjeta(0.0);
                        } else {
                            d.setNetoTarjeta(netPay);
                            d.setNetoTesoreria(0.0);
                        }
                    } else {
                        if (d.getNetoTarjeta() == null) d.setNetoTarjeta(0.0);
                        if (d.getNetoTesoreria() == null) d.setNetoTesoreria(0.0);
                    }

                    checkDoubleFields(d);

                    double netPay = d.getNetoTarjeta() + d.getNetoTesoreria();
                    totalNeto += netPay;
                }
                cabecera.setTotalNeto(totalNeto);
            }
            return repository.save(cabecera);
        }
    }

    private void checkDoubleFields(PlanillaDetalle d) {
        if (d.getLunes() == null) d.setLunes(0.0);
        if (d.getMartes() == null) d.setMartes(0.0);
        if (d.getMiercoles() == null) d.setMiercoles(0.0);
        if (d.getJueves() == null) d.setJueves(0.0);
        if (d.getViernes() == null) d.setViernes(0.0);
        if (d.getSabado() == null) d.setSabado(0.0);
        if (d.getDomingo() == null) d.setDomingo(0.0);
        if (d.getJornal() == null) d.setJornal(0.0);
        if (d.getHeLunes() == null) d.setHeLunes(0.0);
        if (d.getHeMartes() == null) d.setHeMartes(0.0);
        if (d.getHeMiercoles() == null) d.setHeMiercoles(0.0);
        if (d.getHeJueves() == null) d.setHeJueves(0.0);
        if (d.getHeViernes() == null) d.setHeViernes(0.0);
        if (d.getHeSabado() == null) d.setHeSabado(0.0);
        if (d.getCantidadHoraSem() == null) d.setCantidadHoraSem(0.0);
        if (d.getDescManual() == null) d.setDescManual(0.0);
        if (d.getColaboracion() == null) d.setColaboracion(0.0);
        if (d.getDescuentos() == null) d.setDescuentos(0.0);
        if (d.getAnticipo() == null) d.setAnticipo(0.0);
        if (d.getTotalDias() == null) d.setTotalDias(0.0);
        if (d.getImporteDomingo() == null) d.setImporteDomingo(0.0);
        if (d.getTotalHorasExtras() == null) d.setTotalHorasExtras(0.0);
        if (d.getImporteHoraExtra() == null) d.setImporteHoraExtra(0.0);
        if (d.getGrossPay() == null) d.setGrossPay(0.0);
        if (d.getIps() == null) d.setIps(0.0);
        if (d.getNetoTarjeta() == null) d.setNetoTarjeta(0.0);
        if (d.getNetoTesoreria() == null) d.setNetoTesoreria(0.0);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PlanillaCabecera generateInMemoryPlanilla(LocalDate desde, LocalDate hasta) {
        PlanillaCabecera cab = new PlanillaCabecera();
        cab.setFechaDesde(desde);
        cab.setFechaHasta(hasta);
        cab.setFechaPago(LocalDate.now());
        cab.setTipoCarpeta("JORNALEROS");
        cab.setSubcarpetaMes(detectMonthFromNameOrDate("", desde));
        cab.setNombre("Planilla del " + desde.toString() + " al " + hasta.toString());

        // Obtener marcaciones de la semana
        List<Marcacion> marcaciones = marcacionRepository.findByFechaBetween(desde, hasta);

        // Agrupar marcaciones por cédula normalizada
        Map<String, List<Marcacion>> marcacionesPorCedula = marcaciones.stream()
                .filter(m -> m.getCedula() != null)
                .collect(Collectors.groupingBy(m -> com.example.domain.PunteroEnum.normalizeCedula(m.getCedula())));

        // Obtener todos los empleados configurados
        List<EmpleadoSalario> empleados = empleadoSalarioRepository.findAll();
        List<PlanillaDetalle> detalles = new ArrayList<>();

        for (EmpleadoSalario emp : empleados) {
            PlanillaDetalle d = new PlanillaDetalle();
            d.setCabecera(cab);
            d.setCedula(emp.getCedula());
            d.setNombre(emp.getNombre());
            
            // Jornal = tarifa de hora normal * 8
            double jornalCalculado = emp.getPagoHoraNormal() * 8.0;
            d.setJornal(jornalCalculado > 0 ? jornalCalculado : 120000.0);

            // Obtener marcaciones del empleado en esta semana (usando cédula normalizada)
            List<Marcacion> marcacionesSemana = marcacionesPorCedula.getOrDefault(
                    com.example.domain.PunteroEnum.normalizeCedula(emp.getCedula()), new ArrayList<>());
            double totalHorasSemanales = 0.0;

            for (Marcacion m : marcacionesSemana) {
                if (m.getFecha() == null) continue;
                DayOfWeek dow = m.getFecha().getDayOfWeek();
                double horasTrabajadas = m.getHorasTrabajadas() != null ? m.getHorasTrabajadas() : 0.0;
                double horasExtras = m.getHorasExtras() != null ? m.getHorasExtras() : 0.0;

                totalHorasSemanales += horasTrabajadas + horasExtras;

                // Marcar asistencia (1.0 = presente) y guardar horas extras
                switch (dow) {
                    case MONDAY: d.setLunes(1.0); d.setHeLunes(horasExtras); break;
                    case TUESDAY: d.setMartes(1.0); d.setHeMartes(horasExtras); break;
                    case WEDNESDAY: d.setMiercoles(1.0); d.setHeMiercoles(horasExtras); break;
                    case THURSDAY: d.setJueves(1.0); d.setHeJueves(horasExtras); break;
                    case FRIDAY: d.setViernes(1.0); d.setHeViernes(horasExtras); break;
                    case SATURDAY: d.setSabado(1.0); d.setHeSabado(horasExtras); break;
                    case SUNDAY: d.setDomingo(1.0); break;
                }
            }
            d.setCantidadHoraSem(totalHorasSemanales);
            
            // Pre-calcular campos derivados
            double tDias = d.getLunes() + d.getMartes() + d.getMiercoles() + d.getJueves() + d.getViernes() + d.getSabado();
            d.setTotalDias(tDias);
            
            double impDomingo = d.getJornal() / 4.25;
            d.setImporteDomingo(impDomingo * d.getDomingo());
            
            double tHoras = d.getHeLunes() + d.getHeMartes() + d.getHeMiercoles() + d.getHeJueves() + d.getHeViernes() + d.getHeSabado();
            d.setTotalHorasExtras(tHoras);
            
            double impXHora = (d.getJornal() * 1.5) / 8.5;
            d.setImporteHoraExtra(impXHora);
            
            double gp = (tDias * d.getJornal()) + d.getImporteDomingo() + (tHoras * impXHora);
            d.setGrossPay(gp);
            
            double colab = d.getColaboracion() != null ? d.getColaboracion() : 0.0;
            double compSal = d.getAnticipo() != null ? d.getAnticipo() : 0.0;
            double baseImp = gp + colab + compSal;
            double ipsVal = Math.round(baseImp * 0.09);
            d.setIps(ipsVal);
            
            double descManual = d.getDescManual() != null ? d.getDescManual() : 0.0;
            double desc = d.getDescuentos() != null ? d.getDescuentos() : 0.0;
            double netPay = baseImp - ipsVal - descManual - desc;
            if ("EFECTIVO".equals(d.getMetodoPago())) {
                d.setNetoTesoreria(netPay);
                d.setNetoTarjeta(0.0);
            } else {
                d.setNetoTarjeta(netPay);
                d.setNetoTesoreria(0.0);
            }
            
            detalles.add(d);
        }
        
        // Ordenar por nombre del empleado
        detalles.sort(Comparator.comparing(PlanillaDetalle::getNombre));
        
        cab.setDetalles(detalles);
        return cab;
    }
}
