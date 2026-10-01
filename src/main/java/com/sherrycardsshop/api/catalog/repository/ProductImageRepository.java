package com.sherrycardsshop.api.catalog.repository;

import java.util.Collection;
import java.util.List;

import com.sherrycardsshop.api.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    boolean existsByUrl(String url);

    @Query("select i from ProductImage i where i.product.id in :productIds order by i.displayOrder asc, i.id asc")
    List<ProductImage> findByProductIds(@Param("productIds") Collection<Long> productIds);
}
