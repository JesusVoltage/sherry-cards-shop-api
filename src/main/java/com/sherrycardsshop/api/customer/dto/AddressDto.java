package com.sherrycardsshop.api.customer.dto;

public record AddressDto(
        Long id,
        String alias,
        String nombreDestinatario,
        String apellidosDestinatario,
        String telefono,
        String calle,
        String numero,
        String complemento,
        String codigoPostal,
        String localidad,
        String provincia,
        String pais,
        boolean usoEnvio,
        boolean usoFacturacion,
        boolean predeterminadaEnvio,
        boolean predeterminadaFacturacion) {
}
