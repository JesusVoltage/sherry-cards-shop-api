package com.sherrycardsshop.api.customer.entity;

import java.time.LocalDateTime;

import com.sherrycardsshop.api.auth.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dirección de envío y/o facturación de un usuario. Las columnas generadas
 * {@code predeterminada_*_usuario_id} no se mapean: solo existen para que la base de datos
 * garantice una única dirección predeterminada de cada tipo por usuario.
 */
@Entity
@Table(name = "direcciones_usuario")
@Getter
@Setter
@NoArgsConstructor
public class DireccionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(length = 100)
    private String alias;

    @Column(name = "nombre_destinatario", nullable = false, length = 100)
    private String nombreDestinatario;

    @Column(name = "apellidos_destinatario", nullable = false, length = 150)
    private String apellidosDestinatario;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 200)
    private String calle;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(length = 150)
    private String complemento;

    @Column(name = "codigo_postal", nullable = false, length = 20)
    private String codigoPostal;

    @Column(nullable = false, length = 100)
    private String localidad;

    @Column(nullable = false, length = 100)
    private String provincia;

    @Column(nullable = false, length = 100)
    private String pais;

    @Column(name = "uso_envio", nullable = false)
    private boolean usoEnvio;

    @Column(name = "uso_facturacion", nullable = false)
    private boolean usoFacturacion;

    @Column(name = "predeterminada_envio", nullable = false)
    private boolean predeterminadaEnvio;

    @Column(name = "predeterminada_facturacion", nullable = false)
    private boolean predeterminadaFacturacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void setCreationTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void setUpdateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
