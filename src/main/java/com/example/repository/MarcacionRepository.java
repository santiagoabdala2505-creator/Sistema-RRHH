package com.example.repository;

import com.example.domain.Marcacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarcacionRepository extends JpaRepository<Marcacion, Long> {

    Optional<Marcacion> findByCedulaAndFecha(String cedula, LocalDate fecha);

    List<Marcacion> findByFechaBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM Marcacion m WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR LOWER(m.cedula) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND m.fecha >= :startDate " +
           "AND m.fecha <= :endDate " +
           "ORDER BY m.fecha DESC, m.horaEntrada ASC")
    List<Marcacion> searchMarcaciones(
        @Param("searchTerm") String searchTerm,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
