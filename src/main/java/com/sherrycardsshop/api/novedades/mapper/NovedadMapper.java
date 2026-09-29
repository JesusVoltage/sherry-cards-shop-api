package com.sherrycardsshop.api.novedades.mapper;

import java.util.List;

import com.sherrycardsshop.api.novedades.dto.NovedadDto;
import com.sherrycardsshop.api.novedades.entity.Novedad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NovedadMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    NovedadDto toDto(Novedad novedad);

    List<NovedadDto> toDtoList(List<Novedad> novedades);
}