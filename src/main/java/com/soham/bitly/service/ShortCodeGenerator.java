package com.soham.bitly.service;

import com.soham.bitly.repository.ShortUrlRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShortUrlRepository shortUrlRepository;

    public ShortCodeGenerator(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    public String generateUnique() {
        String code;
        int attempts = 0;
        do {
            code = randomCode();
            attempts++;
            if (attempts > 20) {
                throw new IllegalStateException("Unable to generate a unique short code");
            }
        } while (shortUrlRepository.existsByShortCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
