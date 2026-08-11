package com.shop.service;

import com.shop.entity.User;
import com.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WhatsAppVerificationService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    public String generateUniqueSecretCode() {
        for (int attempt = 0; attempt < 40; attempt++) {
            String code = randomCode(6);
            if (!userRepository.existsByWhatsappSecretCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate unique WhatsApp secret code");
    }

    @Transactional
    public String verify(String code, String whatsappId) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(whatsappId)) {
            return "❌ Missing verification code or WhatsApp id";
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        User user = userRepository.findByWhatsappSecretCode(normalizedCode).orElse(null);
        if (user == null) {
            return "❌ Invalid verification code";
        }
        user.setWhatsappId(whatsappId.trim());
        user.setWhatsappSecretCode(null);
        userRepository.save(user);
        return "✅ WhatsApp Verified Successfully. Reply HELP for commands.";
    }

    @Transactional
    public void assignNewSecretAndClearLink(User user) {
        user.setWhatsappSecretCode(generateUniqueSecretCode());
        user.setWhatsappId(null);
        userRepository.save(user);
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
