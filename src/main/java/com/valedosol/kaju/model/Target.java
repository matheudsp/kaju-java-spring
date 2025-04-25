package com.valedosol.kaju.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Target {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String identifier;
    private String type; // "channel" ou "group" ou "newsletter"
    private String description;

    // Constructor
    public Target(String name, String identifier, String type, String description) {
        this.name = name;
        this.identifier = identifier;
        this.type = type;
        this.description = description;
    }
}