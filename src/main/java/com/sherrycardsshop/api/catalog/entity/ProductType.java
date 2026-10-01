package com.sherrycardsshop.api.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_types")
@Getter
@NoArgsConstructor
public class ProductType {

    public static final String SEALED = "SEALED";
    public static final String SINGLE = "SINGLE";
    public static final String ACCESSORY = "ACCESSORY";

    @Id
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;
}
