package com.sherrycardsshop.api.catalog.entity;

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

/** Característica libre de una variante, por ejemplo "Idioma: Español" o "Edición: Promo". */
@Entity
@Table(name = "product_variant_attributes")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariantAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "attribute_name", nullable = false, length = 50)
    private String name;

    @Column(name = "attribute_value", nullable = false, length = 100)
    private String value;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
