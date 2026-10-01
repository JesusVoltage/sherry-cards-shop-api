package com.sherrycardsshop.api.catalog.repository;

import java.util.List;

import com.sherrycardsshop.api.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<Category> findAllByActiveTrueAndParentIsNullOrderByDisplayOrderAsc();
}