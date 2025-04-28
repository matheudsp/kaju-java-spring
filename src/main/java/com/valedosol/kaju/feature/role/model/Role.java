package com.valedosol.kaju.feature.role.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ERole erole = ERole.ROLE_USER;

    public Role(ERole erole) {
        this.erole = erole;
    }
}