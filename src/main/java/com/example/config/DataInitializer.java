package com.example.config;

import com.example.domain.EmpleadoSalario;
import com.example.repository.EmpleadoSalarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EmpleadoSalarioRepository repository;

    @Autowired
    public DataInitializer(EmpleadoSalarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, String> empleados = new LinkedHashMap<>();
        
        // --- Primer Grupo de Capturas ---
        empleados.put("3.318.203", "FELIPE GERMAN JARA GAMARRA");
        empleados.put("4.290.122", "AUGUSTO PASTOR GONZALEZ ROJAS");
        empleados.put("5.189.597", "JOSE ALBERTO MENA PIÑANEZ");
        empleados.put("5.373.319", "LEANDRO EZEQUIEL GONZALEZ RIVEIRO");
        empleados.put("5.940.725", "DANIEL ALEJANDRO VILLANUEVA AYALA");
        empleados.put("5.108.226", "ESTEBAN DARIO LOVERA");
        empleados.put("4.604.684", "JORGE ESTANISLAO SANCHEZ CARDOZO");
        empleados.put("6.255.092", "JOSE MANUEL GIMENEZ");
        empleados.put("4.362.697", "JULIO CESAR PRIETO IRIGOYEN");
        empleados.put("5.852.773", "MILDER ALCIDES ARMOA NUÑEZ");
        empleados.put("4.507.045", "JUAN DAVID MARTINEZ REYES");
        empleados.put("6.153.796", "LAURO BENITEZ FLORES");
        empleados.put("6.829.118", "TOBIAS ALBERTO BRITEZ ESPINOLA");
        empleados.put("3.534.749", "VICENTE TROCHE SALINAS");
        empleados.put("6.809.024", "CRISTIAN RAMON DIAZ VILLALBA");
        empleados.put("7.016.505", "LAZARO DOMINGUEZ BUSTO");
        empleados.put("4.485.035", "HILARIO RAMON PEREIRA ACOSTA");
        
        // --- Segundo Grupo de Capturas ---
        empleados.put("4.338.831", "CRISTHIAN RAMON CACERES MENDOZA");
        empleados.put("4.102.781", "JOSE MIGUEL ORUE BENITEZ");
        empleados.put("1.103.064", "MIGUEL ANGEL IRRAZABAL PIRIS");
        empleados.put("1.840.266", "MARCO ESTEBAN SOSA RODRIGUEZ");
        empleados.put("5.083.206", "DIEGO ARNALDO GAVILAN");
        empleados.put("3.723.893", "CESAR FERREIRA OZUSA");
        empleados.put("706.096", "SANTIAGO PICAGUA ROTELA");
        empleados.put("5.819.955", "SAUL RODOLFO ROMERO NUÑEZ");
        empleados.put("3.945.476", "JUAN ANGEL BRIZUELA ESCOBAR");
        empleados.put("4.552.289", "JORGE MANUEL MACIEL GONZALEZ");
        empleados.put("7.095.883", "FAVIO MENA ZELAYA");
        empleados.put("3.741.495", "RAMON MIRANDA OVELAR");
        empleados.put("6.548.463", "DIEGO GABRIEL MENA ZELAYA");
        empleados.put("7.451.639", "NELSON ESQUIVEL BENITEZ");
        empleados.put("3.665.925", "SINFORIANO BENITEZ CRISTALDO");
        empleados.put("7.575.928", "GUSTAVO ANGEL DELVALLE FERNANDEZ");
        empleados.put("3.511.751", "NICOLAS ANDRES COLMAN DUARTE");
        empleados.put("2.960.963", "LAZARO RIQUELME SOSA");
        empleados.put("6.523.722", "JULIO CESAR RIVEIRO BARRIOS");
        empleados.put("5.194.623", "YENIFER RIVEIRO BARRIOS");
        empleados.put("5.476.481", "DOMINGO GERMAN BARRIOS GONZALEZ");
        empleados.put("4.993.909", "WILLIAN DAVID RIVEIRO MOREL");
        empleados.put("6.999.429", "AMIN ISAIAS DIAZ CABALLERO");

        // Insertar en lote respetando si ya existen (evitar sobreescritura manual previa)
        for (Map.Entry<String, String> entry : empleados.entrySet()) {
            if (!repository.existsById(entry.getKey())) {
                EmpleadoSalario e = new EmpleadoSalario(entry.getKey(), entry.getValue(), 15000.0, 22500.0);
                repository.save(e);
            }
        }
    }
}
