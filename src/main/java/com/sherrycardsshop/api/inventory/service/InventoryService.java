package com.sherrycardsshop.api.inventory.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import com.sherrycardsshop.api.catalog.repository.ProductVariantRepository;
import com.sherrycardsshop.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva y devolución de stock. Es la barrera contra la sobreventa: un pedido solo llega al
 * pago si todas sus líneas se han podido reservar. Además, la restricción
 * {@code ck_product_variants_stock} impide en MySQL cualquier stock negativo.
 */
@Service
public class InventoryService {

    private final ProductVariantRepository variantRepository;

    public InventoryService(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    /**
     * Reserva todas las cantidades o ninguna: si una línea no tiene stock, la excepción deshace
     * la transacción y se devuelven las unidades ya descontadas de las demás.
     *
     * @param quantities unidades por id de variante
     */
    @Transactional
    public void reserve(Map<Long, Integer> quantities) {
        LocalDateTime now = LocalDateTime.now();
        // Mismo orden de bloqueo de filas en todos los pedidos para evitar interbloqueos.
        new TreeMap<>(quantities).forEach((variantId, quantity) -> {
            requirePositive(quantity);
            if (variantRepository.reserveStock(variantId, quantity, now) == 0) {
                throw outOfStock(variantId);
            }
        });
    }

    /** Devuelve stock reservado, por ejemplo al cancelar o caducar un pedido sin pagar. */
    @Transactional
    public void release(Map<Long, Integer> quantities) {
        LocalDateTime now = LocalDateTime.now();
        new TreeMap<>(quantities).forEach((variantId, quantity) -> {
            requirePositive(quantity);
            variantRepository.releaseStock(variantId, quantity, now);
        });
    }

    private ApiException outOfStock(Long variantId) {
        String name = variantRepository.findById(variantId)
                .map(variant -> variant.getProduct().getName() + " (" + variant.getName() + ")")
                .orElse("uno de los productos");
        return new ApiException(HttpStatus.CONFLICT, "No queda stock suficiente de " + name);
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
    }
}
