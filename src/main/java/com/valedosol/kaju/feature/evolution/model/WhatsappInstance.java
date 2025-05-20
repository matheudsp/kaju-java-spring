package com.valedosol.kaju.feature.evolution.model;

import com.valedosol.kaju.feature.auth.model.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "whatsapp_instance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String instanceId;
    
    @Column(nullable = false)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account owner; // null if global instance
    
    @Column(nullable = false)
    private boolean isGlobal;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant lastUsedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstanceStatus status;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = InstanceStatus.DISCONNECTED;
        }
    }
    
    public enum InstanceStatus {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        ERROR
    }
}