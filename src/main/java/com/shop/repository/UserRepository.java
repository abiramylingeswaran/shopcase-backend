package com.shop.repository;

import com.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByWhatsappSecretCode(String whatsappSecretCode);

    Optional<User> findByWhatsappId(String whatsappId);

    boolean existsByWhatsappSecretCode(String whatsappSecretCode);
}
