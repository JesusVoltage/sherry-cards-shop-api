package com.sherrycardsshop.api.catalog.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.sherrycardsshop.api.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("select v.product.id as productId, count(v) as variantCount, min(v.price) as minPrice, "
            + "sum(v.stockQuantity) as totalStock from ProductVariant v where v.product.id in :productIds group by v.product.id")
    List<VariantSummary> summarizeByProductIds(@Param("productIds") Collection<Long> productIds);

    interface VariantSummary {
        Long getProductId();

        long getVariantCount();

        BigDecimal getMinPrice();

        Long getTotalStock();
    }

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
