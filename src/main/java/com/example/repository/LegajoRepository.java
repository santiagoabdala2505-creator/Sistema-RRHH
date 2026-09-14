package com.example.repository;

import com.example.domain.Legajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LegajoRepository extends JpaRepository<Legajo, Long> {
}
