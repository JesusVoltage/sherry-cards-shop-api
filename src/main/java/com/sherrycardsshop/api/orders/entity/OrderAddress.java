package com.sherrycardsshop.api.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Copia de la dirección de envío o facturación en el momento del pedido. */
@Entity
@Table(name = "order_addresses")
@Getter
@Setter
@NoArgsConstructor
public class OrderAddress {

    public static final String SHIPPING = "SHIPPING";
    public static final String BILLING = "BILLING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private CustomerOrder order;

    /** {@link #SHIPPING} o {@link #BILLING}. */
    @Column(name = "address_type", nullable = false, length = 20)
    private String type;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(name = "street_number", nullable = false, length = 30)
    private String streetNumber;

    @Column(name = "address_line2", length = 150)
    private String addressLine2;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(nullable = false, length = 100)
    private String country;
}
