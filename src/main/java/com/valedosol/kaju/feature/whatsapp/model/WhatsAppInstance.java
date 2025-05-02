package com.valedosol.kaju.feature.whatsapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.valedosol.kaju.feature.auth.model.Account;

@Entity
@Table(name = "whatsapp_instances")
@Data
@NoArgsConstructor
public class WhatsAppInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String instanceName;
    
    @Column(nullable = false)
    private String token;  // The token received from Evolution API
    
    @Column
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstanceStatus status = InstanceStatus.DISCONNECTED;
    
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account owner;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime lastConnectedAt;
    
    @Column
    private LocalDateTime lastDisconnectedAt;
    
    public enum InstanceStatus {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        ERROR
    }
    
    public WhatsAppInstance(String instanceName, String token, Account owner) {
        this.instanceName = instanceName;
        this.token = token;
        this.owner = owner;
    }
    
    public WhatsAppInstance(String instanceName, String token, String phoneNumber, Account owner) {
        this.instanceName = instanceName;
        this.token = token;
        this.phoneNumber = phoneNumber;
        this.owner = owner;
    }
}