package com.sherrycardsshop.api.customer.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 100)
        String alias,

        @NotBlank @Size(max = 100)
        String nombreDestinatario,

        @NotBlank @Size(max = 150)
        String apellidosDestinatario,

        @Size(max = 30)
        @Pattern(regexp = "^[0-9 +().-]*$", message = "Usa solo números, espacios y los símbolos + ( ) - .")
        String telefono,

        @NotBlank @Size(max = 200)
        String calle,

        @NotBlank @Size(max = 30)
        String numero,

        @Size(max = 150)
        String complemento,

        @NotBlank @Size(max = 20)
        String codigoPostal,

        @NotBlank @Size(max = 100)
        String localidad,

        @NotBlank @Size(max = 100)
        String provincia,

        @NotBlank @Size(max = 100)
        String pais,

        boolean usoEnvio,
        boolean usoFacturacion,
        boolean predeterminadaEnvio,
        boolean predeterminadaFacturacion) {

    @AssertTrue(message = "Indica si la dirección es de envío, de facturación o de ambos")
    public boolean isUso() {
        return usoEnvio || usoFacturacion;
    }
}
