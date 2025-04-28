package com.valedosol.kaju.feature.target.controller;

import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.target.model.Target;
import com.valedosol.kaju.feature.target.repository.TargetRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/targets")
public class TargetController {

    private final TargetRepository targetRepository;
    private final AccountRepository accountRepository;

    public TargetController(TargetRepository targetRepository, AccountRepository accountRepository) {
        this.targetRepository = targetRepository;
        this.accountRepository = accountRepository;
    }

    // Get all targets (global and user-specific)
    @GetMapping
    public ResponseEntity<List<Target>> getAllTargets() {
        String email = getCurrentUserEmail();
        Account currentUser = accountRepository.findByEmail(email).orElse(null);

        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // Get global targets and user-specific targets
        List<Target> targets = targetRepository.findByOwnerIsNullOrOwnerId(currentUser.getId());
        return new ResponseEntity<>(targets, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTargetById(@PathVariable Long id) {
        return targetRepository.findById(id)
                .map(target -> {
                    // Check if target is global or owned by current user
                    if (target.isGlobal() || isTargetOwnedByCurrentUser(target)) {
                        return new ResponseEntity<Object>(target, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<Object>("Acesso negado", HttpStatus.FORBIDDEN);
                    }
                })
                .orElse(new ResponseEntity<Object>("Target não encontrado", HttpStatus.NOT_FOUND));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Target>> getTargetsByType(@PathVariable String type) {
        String email = getCurrentUserEmail();
        Account currentUser = accountRepository.findByEmail(email).orElse(null);

        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // Filter to include only global targets and user's own targets of the specified
        // type
        List<Target> allTargets = targetRepository.findByType(type);
        List<Target> filteredTargets = allTargets.stream()
                .filter(target -> target.isGlobal()
                        || (target.getOwner() != null && target.getOwner().getId() == currentUser.getId()))
                .toList();

        return new ResponseEntity<>(filteredTargets, HttpStatus.OK);
    }

    // Add a new target associated with the current user
    @PostMapping
    public ResponseEntity<?> createTarget(@RequestBody Target targetRequest) {
        String email = getCurrentUserEmail();
        Account currentUser = accountRepository.findByEmail(email).orElse(null);

        if (currentUser == null) {
            return new ResponseEntity<>("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }

        // Set the owner to the current user
        targetRequest.setOwner(currentUser);

        Target savedTarget = targetRepository.save(targetRequest);
        return new ResponseEntity<>(savedTarget, HttpStatus.CREATED);
    }

    // Delete a target (only if owned by current user)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTarget(@PathVariable Long id) {
        return targetRepository.findById(id)
                .map(target -> {
                    // Only allow deletion of user's own targets, not global ones
                    if (!target.isGlobal() && isTargetOwnedByCurrentUser(target)) {
                        targetRepository.delete(target);
                        return new ResponseEntity<>("Target removido com sucesso", HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>("Não é possível excluir este target", HttpStatus.FORBIDDEN);
                    }
                })
                .orElse(new ResponseEntity<>("Target não encontrado", HttpStatus.NOT_FOUND));
    }

    // Helper methods
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private boolean isTargetOwnedByCurrentUser(Target target) {
        if (target.getOwner() == null)
            return false;

        String email = getCurrentUserEmail();
        Account currentUser = accountRepository.findByEmail(email).orElse(null);

        return currentUser != null && target.getOwner().getId() == currentUser.getId();
    }
}