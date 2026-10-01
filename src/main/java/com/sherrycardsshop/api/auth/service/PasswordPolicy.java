package com.sherrycardsshop.api.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sherrycardsshop.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Política de contraseñas según NIST SP 800-63B: longitud mínima, sin reglas de composición,
 * sin datos de la propia cuenta y sin contraseñas que aparezcan en filtraciones conocidas.
 */
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    // BCrypt solo usa los primeros 72 bytes de la contraseña.
    public static final int MAX_BYTES = 72;
    private static final int MIN_CONTEXT_LENGTH = 4;

    private final BreachedPasswordChecker breachedPasswordChecker;

    public PasswordPolicy(BreachedPasswordChecker breachedPasswordChecker) {
        this.breachedPasswordChecker = breachedPasswordChecker;
    }

    public static boolean fitsBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    /**
     * @param field campo del formulario al que se asocia el error
     */
    public void validate(String password, String email, String username, String field) {
        if (password.length() < MIN_LENGTH) {
            throw invalid("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres", field);
        }
        if (!fitsBcrypt(password)) {
            throw invalid("La contraseña es demasiado larga (máximo " + MAX_BYTES + " caracteres)", field);
        }
        String lower = password.toLowerCase(Locale.ROOT);
        String emailName = email == null ? null : email.substring(0, Math.max(email.indexOf('@'), 0));
        if (contains(lower, username) || contains(lower, emailName)) {
            throw invalid("La contraseña no puede contener tu nombre de usuario ni tu correo", field);
        }
        if (breachedPasswordChecker.isBreached(password)) {
            throw invalid("Esta contraseña aparece en filtraciones de datos conocidas. Elige otra distinta", field);
        }
    }

    private static boolean contains(String lowerPassword, String value) {
        return value != null && value.length() >= MIN_CONTEXT_LENGTH
                && lowerPassword.contains(value.toLowerCase(Locale.ROOT));
    }

    private static ApiException invalid(String message, String field) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, field);
    }
}
