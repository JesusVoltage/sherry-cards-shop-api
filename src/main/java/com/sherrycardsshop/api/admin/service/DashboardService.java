package com.sherrycardsshop.api.admin.service;

import java.util.Map;
import java.util.stream.Collectors;

import com.sherrycardsshop.api.admin.dto.DashboardDto;
import com.sherrycardsshop.api.admin.dto.DashboardDto.ProductCounts;
import com.sherrycardsshop.api.auth.entity.Rol;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.catalog.entity.ProductStatus;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository.StatusCount;
import com.sherrycardsshop.api.media.service.MediaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UsuarioRepository usuarioRepository;
    private final MediaService mediaService;

    public DashboardService(ProductRepository productRepository, CategoryRepository categoryRepository,
                            UsuarioRepository usuarioRepository, MediaService mediaService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.usuarioRepository = usuarioRepository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public DashboardDto summary() {
        Map<String, Long> byStatus = productRepository.countByStatus().stream()
                .collect(Collectors.toMap(StatusCount::getCode, StatusCount::getTotal));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        ProductCounts products = new ProductCounts(total, byStatus.getOrDefault(ProductStatus.ACTIVE, 0L),
                byStatus.getOrDefault(ProductStatus.DRAFT, 0L), byStatus.getOrDefault(ProductStatus.ARCHIVED, 0L));
        return new DashboardDto(products, categoryRepository.count(), usuarioRepository.count(),
                usuarioRepository.countByRolCode(Rol.ADMIN), mediaService.usage());
    }
}
