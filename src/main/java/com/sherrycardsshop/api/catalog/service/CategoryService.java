package com.sherrycardsshop.api.catalog.service;

import java.util.List;

import com.sherrycardsshop.api.catalog.dto.CategoryDto;
import com.sherrycardsshop.api.catalog.mapper.CategoryMapper;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryMapper.toDtoList(categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc());
    }
}