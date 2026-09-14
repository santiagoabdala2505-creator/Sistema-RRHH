package com.example.repository;

import com.example.domain.PlanillaCabecera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanillaCabeceraRepository extends JpaRepository<PlanillaCabecera, Long> {
}
