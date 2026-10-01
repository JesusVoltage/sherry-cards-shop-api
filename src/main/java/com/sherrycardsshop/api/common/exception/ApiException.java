package com.sherrycardsshop.api.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String field;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    /**
     * @param field campo del formulario al que se refiere el error; se devuelve en {@code data}
     *              con el mismo formato que los errores de validación.
     */
    public ApiException(HttpStatus status, String message, String field) {
        super(message);
        this.status = status;
        this.field = field;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getField() {
        return field;
    }
}
