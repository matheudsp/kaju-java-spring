package com.valedosol.kaju.feature.target.model;

import com.valedosol.kaju.feature.auth.model.Account;

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
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account owner; // null indicates a global target available to all users
    
    // Constructor for global targets
    public Target(String name, String identifier, String type, String description) {
        this.name = name;
        this.identifier = identifier;
        this.type = type;
        this.description = description;
        // owner intentionally left null
    }
    
    // Constructor for user-specific targets
    public Target(String name, String identifier, String type, String description, Account owner) {
        this.name = name;
        this.identifier = identifier;
        this.type = type;
        this.description = description;
        this.owner = owner;
    }
    
    // Helper method to check if target is global
    public boolean isGlobal() {
        return owner == null;
    }
}