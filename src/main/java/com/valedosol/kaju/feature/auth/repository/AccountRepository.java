package com.valedosol.kaju.feature.auth.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.valedosol.kaju.feature.auth.model.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);
    Boolean existsByEmail(String email);
}