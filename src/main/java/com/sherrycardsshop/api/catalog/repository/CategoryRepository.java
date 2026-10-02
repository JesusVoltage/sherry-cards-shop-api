package com.sherrycardsshop.api.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.sherrycardsshop.api.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<Category> findAllByActiveTrueAndParentIsNullOrderByDisplayOrderAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByParentId(Long parentId);

    boolean existsByImageUrl(String imageUrl);

    @Query("select coalesce(max(c.displayOrder), -1) from Category c left join c.parent p "
            + "where (:parentId is null and p is null) or p.id = :parentId")
    int maxDisplayOrder(@Param("parentId") Long parentId);
}