package com.valedosol.kaju.feature.evolution.model;

import com.valedosol.kaju.feature.auth.model.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evolution_instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String instanceName;
    
    @Column(nullable = true)
    private String apiToken;
    
    @Column(nullable = true)
    private String phoneNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account owner;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvolutionInstanceStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = true)
    private LocalDateTime lastConnected;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = EvolutionInstanceStatus.DISCONNECTED;
        }
    }
    
    public enum EvolutionInstanceStatus {
        CONNECTED,
        DISCONNECTED,
        PENDING_CONNECTION
    }
}