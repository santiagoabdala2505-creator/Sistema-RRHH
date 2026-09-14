package com.example.repository;

import com.example.domain.EmpleadoSalario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpleadoSalarioRepository extends JpaRepository<EmpleadoSalario, String> {
}
