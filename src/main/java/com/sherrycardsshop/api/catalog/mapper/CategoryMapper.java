package com.sherrycardsshop.api.catalog.mapper;

import java.util.List;

import com.sherrycardsshop.api.catalog.dto.CategoryDto;
import com.sherrycardsshop.api.catalog.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    CategoryDto toDto(Category category);

    List<CategoryDto> toDtoList(List<Category> categories);
}