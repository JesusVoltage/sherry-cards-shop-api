package com.sherrycardsshop.api.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estados_usuario")
@Getter
@NoArgsConstructor
public class EstadoUsuario {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACTIVO = "ACTIVO";
    public static final String BLOQUEADO = "BLOQUEADO";

    @Id
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;
}
