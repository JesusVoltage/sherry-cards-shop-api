package com.sherrycardsshop.api.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.sherrycardsshop.api.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"category", "type", "status"})
    Optional<Product> findDetailedById(Long id);

    /** Búsqueda del panel: {@code search} ya llega en minúsculas y con comodines, o nulo. */
    @Query(value = """
            select p from Product p join fetch p.category join fetch p.type join fetch p.status
            where (:categoryId is null or p.category.id = :categoryId)
              and (:status is null or p.status.code = :status)
              and (:search is null or lower(p.name) like :search or p.slug like :search
                   or exists (select v.id from ProductVariant v where v.product = p and lower(v.sku) like :search))
            order by p.updatedAt desc, p.id desc
            """, countQuery = """
            select count(p) from Product p
            where (:categoryId is null or p.category.id = :categoryId)
              and (:status is null or p.status.code = :status)
              and (:search is null or lower(p.name) like :search or p.slug like :search
                   or exists (select v.id from ProductVariant v where v.product = p and lower(v.sku) like :search))
            """)
    Page<Product> searchForAdmin(@Param("search") String search, @Param("categoryId") Long categoryId,
                                 @Param("status") String status, Pageable pageable);

    @Query("select count(oi) > 0 from OrderItem oi where oi.variant.product.id = :productId")
    boolean hasOrders(@Param("productId") Long productId);

    @Query("select p.status.code as code, count(p) as total from Product p group by p.status.code")
    List<StatusCount> countByStatus();

    interface StatusCount {
        String getCode();

        long getTotal();
    }
}
