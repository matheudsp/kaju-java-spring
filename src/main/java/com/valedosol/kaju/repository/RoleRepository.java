package com.valedosol.kaju.repository;

import com.valedosol.kaju.model.ERole;
import com.valedosol.kaju.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByErole(ERole erole);
}