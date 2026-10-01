package com.sherrycardsshop.api.catalog.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.sherrycardsshop.api.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    /**
     * Descuenta stock en una sola sentencia atómica y solo si hay unidades suficientes.
     * Devuelve 0 si no se pudo reservar; dos compras simultáneas nunca se quedan la misma unidad.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ProductVariant v set v.stockQuantity = v.stockQuantity - :quantity, v.updatedAt = :now "
            + "where v.id = :id and v.active = true and v.stockQuantity >= :quantity")
    int reserveStock(@Param("id") Long id, @Param("quantity") int quantity, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ProductVariant v set v.stockQuantity = v.stockQuantity + :quantity, v.updatedAt = :now where v.id = :id")
    int releaseStock(@Param("id") Long id, @Param("quantity") int quantity, @Param("now") LocalDateTime now);
}
