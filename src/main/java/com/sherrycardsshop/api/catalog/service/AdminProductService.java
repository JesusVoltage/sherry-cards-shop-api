package com.sherrycardsshop.api.catalog.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sherrycardsshop.api.catalog.dto.admin.AdminImageDto;
import com.sherrycardsshop.api.catalog.dto.admin.AdminProductDto;
import com.sherrycardsshop.api.catalog.dto.admin.AdminProductSummaryDto;
import com.sherrycardsshop.api.catalog.dto.admin.AdminVariantDto;
import com.sherrycardsshop.api.catalog.dto.admin.CatalogOptionsDto;
import com.sherrycardsshop.api.catalog.dto.admin.CatalogOptionsDto.CategoryOption;
import com.sherrycardsshop.api.catalog.dto.admin.CatalogOptionsDto.CodeName;
import com.sherrycardsshop.api.catalog.dto.admin.ImageRequest;
import com.sherrycardsshop.api.catalog.dto.admin.ProductRequest;
import com.sherrycardsshop.api.catalog.dto.admin.VariantRequest;
import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.entity.Product;
import com.sherrycardsshop.api.catalog.entity.ProductImage;
import com.sherrycardsshop.api.catalog.entity.ProductStatus;
import com.sherrycardsshop.api.catalog.entity.ProductVariant;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import com.sherrycardsshop.api.catalog.repository.ProductImageRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository;
import com.sherrycardsshop.api.catalog.repository.ProductStatusRepository;
import com.sherrycardsshop.api.catalog.repository.ProductTypeRepository;
import com.sherrycardsshop.api.catalog.repository.ProductVariantRepository;
import com.sherrycardsshop.api.catalog.repository.ProductVariantRepository.VariantSummary;
import com.sherrycardsshop.api.common.dto.PageDto;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.common.util.Slugs;
import com.sherrycardsshop.api.media.service.MediaService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de productos desde el panel de control: alta, edición, listado y borrado. */
@Service
public class AdminProductService {

    static final int MAX_PAGE_SIZE = 100;
    private static final int SLUG_LENGTH = 200;
    private static final int SKU_BASE_LENGTH = 56;

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository typeRepository;
    private final ProductStatusRepository statusRepository;
    private final MediaService mediaService;

    public AdminProductService(ProductRepository productRepository, ProductVariantRepository variantRepository,
                               ProductImageRepository imageRepository, CategoryRepository categoryRepository,
                               ProductTypeRepository typeRepository, ProductStatusRepository statusRepository,
                               MediaService mediaService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
        this.statusRepository = statusRepository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public PageDto<AdminProductSummaryDto> list(String search, Long categoryId, String status, int page, int size) {
        String pattern = search == null || search.isBlank() ? null : "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
        String statusCode = status == null || status.isBlank() ? null : status.strip().toUpperCase(Locale.ROOT);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageDto.of(productRepository.searchForAdmin(pattern, categoryId, statusCode, pageable), this::toSummaries);
    }

    @Transactional(readOnly = true)
    public AdminProductDto get(Long id) {
        return toDto(findProduct(id));
    }

    @Transactional
    public AdminProductDto create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return save(product);
    }

    @Transactional
    public AdminProductDto update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        apply(product, request);
        return save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        if (productRepository.hasOrders(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Este producto tiene pedidos. Archívalo en lugar de borrarlo");
        }
        List<String> imageUrls = product.getImages().stream().map(ProductImage::getUrl).toList();
        productRepository.delete(product);
        mediaService.releaseAfterCommit(imageUrls);
    }

    @Transactional(readOnly = true)
    public CatalogOptionsDto options() {
        List<Category> categories = categoryRepository.findAll();
        Map<Long, Category> byId = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        List<CategoryOption> categoryOptions = categories.stream()
                .map(category -> new CategoryOption(category.getId(), category.getName(), path(category, byId), category.isActive()))
                .sorted(Comparator.comparing(CategoryOption::path, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<CodeName> types = typeRepository.findAll().stream()
                .sorted(Comparator.comparing(type -> type.getId()))
                .map(type -> new CodeName(type.getCode(), type.getName())).toList();
        List<CodeName> statuses = statusRepository.findAll().stream()
                .sorted(Comparator.comparing(status -> status.getId()))
                .map(status -> new CodeName(status.getCode(), status.getName())).toList();
        return new CatalogOptionsDto(categoryOptions, types, statuses);
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().strip());
        product.setDescription(blankToNull(request.description()));
        product.setReleaseDate(request.releaseDate());
        product.setCategory(categoryRepository.findById(request.categoryId()).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "La categoría no existe", "categoryId")));
        product.setType(typeRepository.findByCode(request.type().strip().toUpperCase(Locale.ROOT)).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "Tipo de producto no válido", "type")));
        product.setStatus(statusRepository.findByCode(request.status().strip().toUpperCase(Locale.ROOT)).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "Estado no válido", "status")));
        product.setSlug(resolveSlug(product, request));
        syncVariants(product, request.variants());
        syncImages(product, request.images() == null ? List.of() : request.images());

        boolean sellable = product.getVariants().stream().anyMatch(ProductVariant::isActive);
        if (ProductStatus.ACTIVE.equals(product.getStatus().getCode()) && !sellable) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un producto activo necesita al menos una variante activa", "status");
        }
    }

    private AdminProductDto save(Product product) {
        try {
            return toDto(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException exception) {
            // Otra petición se quedó el mismo slug o SKU entre la comprobación y el guardado.
            throw new ApiException(HttpStatus.CONFLICT, "El slug o algún SKU ya está en uso. Revísalos y vuelve a guardar");
        }
    }

    /** Con slug lo normaliza y exige que esté libre; sin él lo deriva del nombre y le añade -2, -3… si hace falta. */
    private String resolveSlug(Product product, ProductRequest request) {
        Long id = product.getId() == null ? -1L : product.getId();
        boolean requested = request.slug() != null && !request.slug().isBlank();
        String base = Slugs.slugify(requested ? request.slug() : request.name(), SLUG_LENGTH);
        if (base.isEmpty()) {
            if (requested) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El slug necesita letras o números", "slug");
            }
            base = "producto";
        }
        if (!productRepository.existsBySlugAndIdNot(base, id)) {
            return base;
        }
        if (requested) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese slug ya lo usa otro producto", "slug");
        }
        for (int suffix = 2; ; suffix++) {
            String tail = "-" + suffix;
            String candidate = truncate(base, SLUG_LENGTH - tail.length()) + tail;
            if (!productRepository.existsBySlugAndIdNot(candidate, id)) {
                return candidate;
            }
        }
    }

    private void syncVariants(Product product, List<VariantRequest> requests) {
        Map<Long, ProductVariant> remaining = new HashMap<>();
        product.getVariants().forEach(variant -> remaining.put(variant.getId(), variant));
        Set<String> skus = new HashSet<>();
        List<ProductVariant> kept = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            VariantRequest request = requests.get(i);
            String field = "variants[" + i + "]";
            ProductVariant variant = request.id() == null ? new ProductVariant() : remaining.remove(request.id());
            if (variant == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La variante no pertenece a este producto", field + ".id");
            }
            // Sin SKU, una variante existente conserva el suyo y una nueva recibe uno generado al final.
            String sku = request.sku() == null || request.sku().isBlank() ? variant.getSku() : request.sku().strip().toUpperCase(Locale.ROOT);
            if (sku != null) {
                if (!skus.add(sku)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "SKU repetido en otra variante", field + ".sku");
                }
                if (!sku.equals(variant.getSku()) && variantRepository.existsBySku(sku)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Ese SKU ya existe", field + ".sku");
                }
            }
            if (request.compareAtPrice() != null && request.compareAtPrice().compareTo(request.price()) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El precio anterior debe ser mayor que el precio", field + ".compareAtPrice");
            }
            variant.setSku(sku);
            variant.setName(request.name().strip());
            variant.setPrice(request.price());
            variant.setCompareAtPrice(request.compareAtPrice());
            variant.setVatRate(request.vatRate());
            variant.setStockQuantity(request.stockQuantity());
            variant.setWeightGrams(request.weightGrams());
            variant.setActive(request.active());
            variant.setDisplayOrder(i);
            kept.add(variant);
        }

        // Antes de asociarlas al producto: cada consulta de SKU vacía la sesión y una variante sin SKU no se puede guardar.
        for (int i = 0; i < kept.size(); i++) {
            if (kept.get(i).getSku() == null) {
                kept.get(i).setSku(generateSku(product.getSlug(), i + 1, skus));
            }
        }
        // Las variantes que no llegan se borran: el carrito las suelta en cascada y los pedidos guardan su copia.
        product.getVariants().removeIf(variant -> remaining.containsKey(variant.getId()));
        for (ProductVariant variant : kept) {
            if (variant.getProduct() == null) {
                product.addVariant(variant);
            }
        }
        product.getVariants().sort(Comparator.comparingInt(ProductVariant::getDisplayOrder));
    }

    private String generateSku(String slug, int start, Set<String> taken) {
        String base = truncate(slug.toUpperCase(Locale.ROOT), SKU_BASE_LENGTH).replaceAll("-+$", "");
        for (int number = start; ; number++) {
            String candidate = base + "-" + number;
            if (taken.add(candidate) && !variantRepository.existsBySku(candidate)) {
                return candidate;
            }
        }
    }

    private void syncImages(Product product, List<ImageRequest> requests) {
        Set<String> previous = product.getImages().stream().map(ProductImage::getUrl).collect(Collectors.toSet());
        Map<String, ImageRequest> unique = new LinkedHashMap<>();
        requests.forEach(request -> unique.putIfAbsent(request.url().strip(), request));

        product.getImages().clear();
        int order = 0;
        for (Map.Entry<String, ImageRequest> entry : unique.entrySet()) {
            ProductImage image = new ProductImage();
            image.setUrl(entry.getKey());
            image.setAltText(blankToNull(entry.getValue().altText()));
            image.setDisplayOrder(order++);
            product.addImage(image);
        }
        previous.removeAll(unique.keySet());
        mediaService.releaseAfterCommit(previous);
    }

    private Product findProduct(Long id) {
        return productRepository.findDetailedById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private List<AdminProductSummaryDto> toSummaries(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, VariantSummary> variants = variantRepository.summarizeByProductIds(ids).stream()
                .collect(Collectors.toMap(VariantSummary::getProductId, Function.identity()));
        Map<Long, String> firstImage = new HashMap<>();
        imageRepository.findByProductIds(ids).forEach(image -> firstImage.putIfAbsent(image.getProduct().getId(), image.getUrl()));

        return products.stream().map(product -> {
            VariantSummary summary = variants.get(product.getId());
            return new AdminProductSummaryDto(product.getId(), product.getName(), product.getSlug(),
                    product.getCategory().getId(), product.getCategory().getName(), product.getType().getCode(),
                    product.getStatus().getCode(), summary == null ? 0 : summary.getVariantCount(),
                    summary == null ? null : summary.getMinPrice(),
                    summary == null || summary.getTotalStock() == null ? 0 : summary.getTotalStock(),
                    firstImage.get(product.getId()), product.getUpdatedAt());
        }).toList();
    }

    private static AdminProductDto toDto(Product product) {
        List<AdminVariantDto> variants = product.getVariants().stream()
                .map(variant -> new AdminVariantDto(variant.getId(), variant.getSku(), variant.getName(), variant.getPrice(),
                        variant.getCompareAtPrice(), variant.getVatRate(), variant.getStockQuantity(),
                        variant.getWeightGrams(), variant.isActive()))
                .toList();
        List<AdminImageDto> images = product.getImages().stream()
                .map(image -> new AdminImageDto(image.getId(), image.getUrl(), image.getAltText()))
                .toList();
        return new AdminProductDto(product.getId(), product.getName(), product.getSlug(), product.getCategory().getId(),
                product.getType().getCode(), product.getStatus().getCode(), product.getDescription(),
                product.getReleaseDate(), variants, images, product.getCreatedAt(), product.getUpdatedAt());
    }

    private static String path(Category category, Map<Long, Category> byId) {
        List<String> names = new ArrayList<>();
        Category current = category;
        // El límite evita un bucle infinito si alguna vez quedara un ciclo en los datos.
        for (int depth = 0; current != null && depth < CategoryService.MAX_DEPTH; depth++) {
            names.addFirst(current.getName());
            current = current.getParent() == null ? null : byId.get(current.getParent().getId());
        }
        return String.join(" › ", names);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
