package com.sherrycardsshop.api.orders.repository;

import java.math.BigDecimal;

import com.sherrycardsshop.api.auth.entity.Usuario;
import com.sherrycardsshop.api.auth.repository.EstadoUsuarioRepository;
import com.sherrycardsshop.api.auth.repository.RolRepository;
import com.sherrycardsshop.api.auth.repository.UsuarioRepository;
import com.sherrycardsshop.api.orders.entity.CustomerOrder;
import com.sherrycardsshop.api.orders.entity.OrderAddress;
import com.sherrycardsshop.api.orders.entity.OrderItem;
import com.sherrycardsshop.api.orders.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerOrderRepositoryTest {

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private OrderStatusRepository statusRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EstadoUsuarioRepository estadoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAnOrderWithItsSnapshotsAndStatusHistory() {
        Usuario user = new Usuario();
        user.setEmail("ash@example.com");
        user.setUsername("ash_orders");
        user.setNombre("Ash");
        user.setPasswordHash("{noop}x");
        user.setRol(rolRepository.findByCode("CLIENTE").orElseThrow());
        user.setEstado(estadoRepository.findByCode("ACTIVO").orElseThrow());
        usuarioRepository.save(user);

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber("SCS-2026-000001");
        order.setUser(user);
        order.setEmail(user.getEmail());
        order.changeStatus(statusRepository.findByCode(OrderStatus.PENDING_PAYMENT).orElseThrow(), "Pedido creado", user);

        OrderItem item = new OrderItem();
        item.setSku("ETB-ES");
        item.setProductName("Elite Trainer Box");
        item.setVariantName("Español");
        item.setUnitPrice(new BigDecimal("59.95"));
        item.setVatRate(new BigDecimal("21.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("119.90"));
        order.addItem(item);

        OrderAddress shipping = new OrderAddress();
        shipping.setType(OrderAddress.SHIPPING);
        shipping.setFirstName("Ash");
        shipping.setLastName("Ketchum");
        shipping.setStreet("Calle Mayor");
        shipping.setStreetNumber("1");
        shipping.setPostalCode("28013");
        shipping.setCity("Madrid");
        shipping.setProvince("Madrid");
        shipping.setCountry("España");
        order.addAddress(shipping);
        order.setSubtotal(new BigDecimal("119.90"));
        order.setTaxTotal(new BigDecimal("20.81"));
        order.setTotal(new BigDecimal("119.90"));
        orderRepository.saveAndFlush(order);
        entityManager.clear();

        CustomerOrder stored = orderRepository.findByOrderNumber("SCS-2026-000001").orElseThrow();
        assertThat(stored.getStatus().getCode()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(stored.getItems()).singleElement().satisfies(line -> {
            assertThat(line.getVariant()).isNull();
            assertThat(line.getLineTotal()).isEqualByComparingTo("119.90");
        });
        assertThat(stored.getAddresses()).extracting(OrderAddress::getType).containsExactly(OrderAddress.SHIPPING);
        assertThat(stored.getStatusHistory()).singleElement()
                .satisfies(entry -> assertThat(entry.getNote()).isEqualTo("Pedido creado"));
        assertThat(stored.getPlacedAt()).isNotNull();
    }
}
