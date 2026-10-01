package com.sherrycardsshop.api.customer.mapper;

import java.util.List;

import com.sherrycardsshop.api.customer.dto.AddressDto;
import com.sherrycardsshop.api.customer.entity.DireccionUsuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressDto toDto(DireccionUsuario direccion);

    List<AddressDto> toDtoList(List<DireccionUsuario> direcciones);
}
