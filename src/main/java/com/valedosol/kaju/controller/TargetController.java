package com.valedosol.kaju.controller;

import com.valedosol.kaju.model.Target;
import com.valedosol.kaju.repository.TargetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/targets")
public class TargetController {

    private final TargetRepository targetRepository;

    public TargetController(TargetRepository targetRepository) {
        this.targetRepository = targetRepository;
    }

    @GetMapping
    public ResponseEntity<List<Target>> getAllTargets() {
        List<Target> targets = targetRepository.findAll();
        return new ResponseEntity<>(targets, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTargetById(@PathVariable Long id) {
        return targetRepository.findById(id)
                .map(target -> new ResponseEntity<Object>(target, HttpStatus.OK))
                .orElse(new ResponseEntity<Object>("Target não encontrado", HttpStatus.NOT_FOUND));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Target>> getTargetsByType(@PathVariable String type) {
        List<Target> targets = targetRepository.findByType(type);
        return new ResponseEntity<>(targets, HttpStatus.OK);
    }
}