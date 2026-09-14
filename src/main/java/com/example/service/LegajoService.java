package com.example.service;

import com.example.domain.Legajo;
import com.example.repository.LegajoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LegajoService {

    private final LegajoRepository repository;

    @Autowired
    public LegajoService(LegajoRepository repository) {
        this.repository = repository;
    }

    public List<Legajo> findAll() {
        return repository.findAll();
    }

    public Optional<Legajo> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Legajo save(String nombre, MultipartFile file, String usuario) throws IOException {
        Legajo legajo = new Legajo();
        legajo.setNombre(nombre);
        legajo.setNombreArchivo(file.getOriginalFilename());
        legajo.setArchivoDatos(file.getBytes());
        legajo.setFechaSubida(LocalDateTime.now());
        legajo.setUsuarioSubida(usuario);
        return repository.save(legajo);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
