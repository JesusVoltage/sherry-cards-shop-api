package com.sherrycardsshop.api.auth.mapper;

import com.sherrycardsshop.api.auth.dto.UserDto;
import com.sherrycardsshop.api.auth.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "rol.code")
    @Mapping(target = "status", source = "estado.code")
    @Mapping(target = "emailVerifiedAt", source = "emailVerificadoAt")
    @Mapping(target = "lastAccessAt", source = "ultimoAccesoAt")
    UserDto toDto(Usuario usuario);
}
