package com.sherrycardsshop.api.catalog.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sherrycardsshop.api.catalog.dto.admin.AdminCategoryDto;
import com.sherrycardsshop.api.catalog.dto.admin.CategoryReorderRequest;
import com.sherrycardsshop.api.catalog.dto.admin.CategoryRequest;
import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository.CategoryCount;
import com.sherrycardsshop.api.common.exception.ApiException;
import com.sherrycardsshop.api.common.util.Slugs;
import com.sherrycardsshop.api.media.service.MediaService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Árbol de categorías desde el panel. El catálogo es pequeño, así que las comprobaciones de
 * ciclos y profundidad se hacen en memoria con todas las categorías cargadas.
 */
@Service
public class AdminCategoryService {

    private static final int SLUG_LENGTH = 100;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MediaService mediaService;

    public AdminCategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
                                MediaService mediaService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryDto> list() {
        List<Category> categories = categoryRepository.findAll();
        Map<Long, Long> products = productRepository.countByCategory().stream()
                .collect(Collectors.toMap(CategoryCount::getCategoryId, CategoryCount::getTotal));
        Map<Long, Long> children = categories.stream().filter(category -> category.getParent() != null)
                .collect(Collectors.groupingBy(category -> category.getParent().getId(), Collectors.counting()));
        return categories.stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder).thenComparing(Category::getId))
                .map(category -> toDto(category, products.getOrDefault(category.getId(), 0L),
                        children.getOrDefault(category.getId(), 0L)))
                .toList();
    }

    @Transactional
    public AdminCategoryDto create(CategoryRequest request) {
        Category category = new Category();
        Category parent = resolveParent(category, request.parentId(), all());
        apply(category, request, parent);
        category.setDisplayOrder(categoryRepository.maxDisplayOrder(request.parentId()) + 1);
        return save(category, 0, 0);
    }

    @Transactional
    public AdminCategoryDto update(Long id, CategoryRequest request) {
        Map<Long, Category> all = all();
        Category category = find(all, id);
        Long previousParent = parentId(category);
        String previousImage = category.getImageUrl();
        if (category.isSystem() && request.parentId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sin categoría tiene que quedarse en la raíz", "parentId");
        }
        Category parent = resolveParent(category, request.parentId(), all);
        apply(category, request, parent);
        if (!Objects.equals(previousParent, request.parentId())) {
            category.setDisplayOrder(categoryRepository.maxDisplayOrder(request.parentId()) + 1);
        }
        if (previousImage != null && !previousImage.equals(category.getImageUrl())) {
            mediaService.releaseAfterCommit(List.of(previousImage));
        }
        long children = all.values().stream().filter(other -> Objects.equals(parentId(other), id)).count();
        return save(category, productCount(id), children);
    }

    /** Devuelve cuántos productos se han pasado a "Sin categoría". */
    @Transactional
    public int delete(Long id) {
        Category category = find(all(), id);
        if (category.isSystem()) {
            throw new ApiException(HttpStatus.CONFLICT, "Sin categoría no se puede borrar");
        }
        if (categoryRepository.existsByParentId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Tiene subcategorías: muévelas o bórralas antes");
        }
        Category fallback = categoryRepository.findBySlug(Category.UNCATEGORIZED_SLUG).orElseThrow(() ->
                new IllegalStateException("Falta la categoría " + Category.UNCATEGORIZED_SLUG));
        int moved = productRepository.moveToCategory(id, fallback);
        String image = category.getImageUrl();
        categoryRepository.deleteById(id);
        if (image != null) {
            mediaService.releaseAfterCommit(List.of(image));
        }
        return moved;
    }

    @Transactional
    public List<AdminCategoryDto> reorder(CategoryReorderRequest request) {
        List<Category> siblings = all().values().stream()
                .filter(category -> Objects.equals(parentId(category), request.parentId())).toList();
        Set<Long> expected = siblings.stream().map(Category::getId).collect(Collectors.toSet());
        if (!expected.equals(new HashSet<>(request.categoryIds())) || expected.size() != request.categoryIds().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La lista no coincide con las categorías de ese nivel");
        }
        Map<Long, Category> byId = siblings.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        for (int i = 0; i < request.categoryIds().size(); i++) {
            byId.get(request.categoryIds().get(i)).setDisplayOrder(i);
        }
        categoryRepository.flush();
        return list();
    }

    private void apply(Category category, CategoryRequest request, Category parent) {
        category.setName(request.name().strip());
        category.setDescription(blankToNull(request.description()));
        category.setImageUrl(blankToNull(request.imageUrl()));
        category.setActive(request.active());
        category.setParent(parent);
        if (category.isSystem()) {
            if (request.slug() != null && !request.slug().isBlank()
                    && !Category.UNCATEGORIZED_SLUG.equals(Slugs.slugify(request.slug(), SLUG_LENGTH))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La dirección de Sin categoría no se puede cambiar", "slug");
            }
            return;
        }
        category.setSlug(resolveSlug(category, request));
    }

    /**
     * Valida el nuevo padre: que exista, que no sea la propia categoría ni una de sus
     * subcategorías (crearía un ciclo) y que el árbol no pase del máximo de niveles.
     */
    private Category resolveParent(Category category, Long parentId, Map<Long, Category> all) {
        if (parentId == null) {
            return null;
        }
        Category parent = all.get(parentId);
        if (parent == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La categoría padre no existe", "parentId");
        }
        if (parent.isSystem()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pueden crear subcategorías dentro de Sin categoría", "parentId");
        }
        Map<Long, List<Category>> childrenByParent = new HashMap<>();
        all.values().forEach(other -> childrenByParent.computeIfAbsent(parentId(other), ignored -> new ArrayList<>()).add(other));
        if (category.getId() != null && (parent.getId().equals(category.getId())
                || descendants(category.getId(), childrenByParent).contains(parent.getId()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una categoría no puede ir dentro de sí misma ni de sus subcategorías", "parentId");
        }
        int subtreeHeight = category.getId() == null ? 1 : height(category.getId(), childrenByParent, 1);
        if (depth(parent, all) + subtreeHeight > CategoryService.MAX_DEPTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Como máximo puede haber " + CategoryService.MAX_DEPTH + " niveles", "parentId");
        }
        return parent;
    }

    private String resolveSlug(Category category, CategoryRequest request) {
        Long id = category.getId() == null ? -1L : category.getId();
        boolean requested = request.slug() != null && !request.slug().isBlank();
        String base = Slugs.slugify(requested ? request.slug() : request.name(), SLUG_LENGTH);
        if (base.isEmpty()) {
            if (requested) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El slug necesita letras o números", "slug");
            }
            base = "categoria";
        }
        if (!categoryRepository.existsBySlugAndIdNot(base, id)) {
            return base;
        }
        if (requested) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese slug ya lo usa otra categoría", "slug");
        }
        for (int suffix = 2; ; suffix++) {
            String tail = "-" + suffix;
            String candidate = (base.length() + tail.length() > SLUG_LENGTH ? base.substring(0, SLUG_LENGTH - tail.length()) : base) + tail;
            if (!categoryRepository.existsBySlugAndIdNot(candidate, id)) {
                return candidate;
            }
        }
    }

    private AdminCategoryDto save(Category category, long productCount, long childCount) {
        try {
            return toDto(categoryRepository.saveAndFlush(category), productCount, childCount);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese slug ya lo usa otra categoría", "slug");
        }
    }

    private Map<Long, Category> all() {
        return categoryRepository.findAll().stream().collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private static Category find(Map<Long, Category> all, Long id) {
        Category category = all.get(id);
        if (category == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Categoría no encontrada");
        }
        return category;
    }

    private long productCount(Long categoryId) {
        return productRepository.countByCategory().stream()
                .filter(count -> count.getCategoryId().equals(categoryId))
                .mapToLong(CategoryCount::getTotal).findFirst().orElse(0);
    }

    private static Set<Long> descendants(Long id, Map<Long, List<Category>> childrenByParent) {
        Set<Long> found = new HashSet<>();
        List<Long> pending = new ArrayList<>(List.of(id));
        while (!pending.isEmpty()) {
            for (Category child : childrenByParent.getOrDefault(pending.removeLast(), List.of())) {
                if (found.add(child.getId())) {
                    pending.add(child.getId());
                }
            }
        }
        return found;
    }

    /** Niveles de la rama que cuelga de {@code id}, contando la propia categoría (1 si no tiene hijas). */
    private static int height(Long id, Map<Long, List<Category>> childrenByParent, int level) {
        if (level > CategoryService.MAX_DEPTH) {
            // Una rama tan profunda (o un ciclo en los datos) no cabe bajo ningún padre.
            return CategoryService.MAX_DEPTH + 1;
        }
        int tallest = 0;
        for (Category child : childrenByParent.getOrDefault(id, List.of())) {
            tallest = Math.max(tallest, height(child.getId(), childrenByParent, level + 1));
        }
        return tallest + 1;
    }

    /** Nivel de la categoría en el árbol: 1 para las raíces. */
    private static int depth(Category category, Map<Long, Category> all) {
        int depth = 1;
        Long parent = parentId(category);
        while (parent != null && depth <= CategoryService.MAX_DEPTH) {
            depth++;
            parent = parentId(all.get(parent));
        }
        return depth;
    }

    private static Long parentId(Category category) {
        return category == null || category.getParent() == null ? null : category.getParent().getId();
    }

    private static AdminCategoryDto toDto(Category category, long productCount, long childCount) {
        return new AdminCategoryDto(category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                category.getImageUrl(), parentId(category), category.isActive(), category.getDisplayOrder(),
                productCount, childCount, category.isSystem());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
