package com.sherrycardsshop.api.admin.dto;

import java.util.List;

import com.sherrycardsshop.api.catalog.dto.admin.CatalogOptionsDto.CodeName;

public record UserOptionsDto(List<CodeName> roles, List<CodeName> statuses) {
}
