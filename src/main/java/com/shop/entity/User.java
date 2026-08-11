package com.shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String phone;

    /** One-time code for WhatsApp VERIFY <code> bot linking */
    @Column(name = "whatsapp_secret_code", length = 32)
    private String whatsappSecretCode;

    /** WhatsApp chat id from Node after VERIFY (e.g. 9470…@c.us) */
    @Column(name = "whatsapp_id", length = 128)
    private String whatsappId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
