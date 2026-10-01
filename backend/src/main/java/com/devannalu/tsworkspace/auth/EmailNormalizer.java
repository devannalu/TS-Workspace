package com.devannalu.tsworkspace.auth;

import java.util.Locale;

public final class EmailNormalizer {
    private EmailNormalizer() { }

    public static String normalize(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("E-mail obrigatório.");
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
