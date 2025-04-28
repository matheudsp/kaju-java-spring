package com.valedosol.kaju.feature.role.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.valedosol.kaju.feature.role.model.ERole;
import com.valedosol.kaju.feature.role.model.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByErole(ERole erole);
}