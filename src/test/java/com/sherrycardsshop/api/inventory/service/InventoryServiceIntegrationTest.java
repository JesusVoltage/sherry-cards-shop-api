package com.sherrycardsshop.api.inventory.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sherrycardsshop.api.catalog.entity.Category;
import com.sherrycardsshop.api.catalog.entity.Product;
import com.sherrycardsshop.api.catalog.entity.ProductStatus;
import com.sherrycardsshop.api.catalog.entity.ProductType;
import com.sherrycardsshop.api.catalog.entity.ProductVariant;
import com.sherrycardsshop.api.catalog.repository.CategoryRepository;
import com.sherrycardsshop.api.catalog.repository.ProductRepository;
import com.sherrycardsshop.api.catalog.repository.ProductStatusRepository;
import com.sherrycardsshop.api.catalog.repository.ProductTypeRepository;
import com.sherrycardsshop.api.catalog.repository.ProductVariantRepository;
import com.sherrycardsshop.api.common.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductTypeRepository typeRepository;

    @Autowired
    private ProductStatusRepository statusRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> categoryIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        variantRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllById(categoryIds);
    }

    @Test
    void reservesAndReleasesStock() {
        ProductVariant variant = createVariant("ETB-ES", 5);

        inventoryService.reserve(Map.of(variant.getId(), 3));
        assertThat(stockOf(variant)).isEqualTo(2);

        inventoryService.release(Map.of(variant.getId(), 3));
        assertThat(stockOf(variant)).isEqualTo(5);
    }

    @Test
    void rejectsReservationsBeyondStockWithoutChangingIt() {
        ProductVariant variant = createVariant("BOOSTER-ES", 2);

        assertThatThrownBy(() -> inventoryService.reserve(Map.of(variant.getId(), 3)))
                .isInstanceOf(ApiException.class)
                .hasMessage("No queda stock suficiente de Caja de prueba BOOSTER-ES (Español)");
        assertThat(stockOf(variant)).isEqualTo(2);
    }

    @Test
    void reservesAllLinesOrNone() {
        ProductVariant available = createVariant("PROMO-A", 10);
        ProductVariant scarce = createVariant("PROMO-B", 1);

        assertThatThrownBy(() -> inventoryService.reserve(Map.of(available.getId(), 4, scarce.getId(), 2)))
                .isInstanceOf(ApiException.class);
        assertThat(stockOf(available)).isEqualTo(10);
        assertThat(stockOf(scarce)).isEqualTo(1);
    }

    @Test
    void neverSellsMoreUnitsThanAvailableUnderConcurrency() throws Exception {
        ProductVariant variant = createVariant("LAUNCH-ES", 2);
        int buyers = 10;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        Callable<Boolean> buyOne = () -> {
            start.await();
            try {
                inventoryService.reserve(Map.of(variant.getId(), 1));
                return true;
            } catch (ApiException outOfStock) {
                return false;
            }
        };
        for (int buyer = 0; buyer < buyers; buyer++) {
            results.add(pool.submit(buyOne));
        }
        start.countDown();

        int sold = 0;
        for (Future<Boolean> result : results) {
            sold += result.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(sold).isEqualTo(2);
        assertThat(stockOf(variant)).isZero();
    }

    @Test
    void databaseRejectsNegativeStock() {
        ProductVariant variant = createVariant("GUARD-ES", 1);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE product_variants SET stock_quantity = -1 WHERE id = ?", variant.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ProductVariant createVariant(String sku, int stock) {
        Category category = new Category();
        category.setName("Categoría " + sku);
        category.setSlug("inventory-test-" + sku.toLowerCase());
        category.setActive(true);
        category = categoryRepository.saveAndFlush(category);
        categoryIds.add(category.getId());

        ProductType type = typeRepository.findByCode(ProductType.SEALED).orElseThrow();
        ProductStatus status = statusRepository.findByCode(ProductStatus.ACTIVE).orElseThrow();
        Product product = new Product();
        product.setCategory(category);
        product.setType(type);
        product.setStatus(status);
        product.setName("Caja de prueba " + sku);
        product.setSlug("caja-" + sku.toLowerCase());

        ProductVariant variant = new ProductVariant();
        variant.setSku(sku);
        variant.setName("Español");
        variant.setPrice(new BigDecimal("149.95"));
        variant.setStockQuantity(stock);
        variant.addAttribute("Idioma", "Español");
        product.addVariant(variant);
        productRepository.saveAndFlush(product);
        return variant;
    }

    private int stockOf(ProductVariant variant) {
        return variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
    }
}
