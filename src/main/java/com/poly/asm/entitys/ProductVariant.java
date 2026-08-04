package com.poly.asm.entitys;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ProductVariants", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "size", "color"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Thêm getter cho product_id
    @Column(name = "product_id", insertable = false, updatable = false)
    private Long productId;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    // Getter và Setter cho productId (tùy chọn nếu không cần set)
    public Long getProductId() { return productId; }
    // Không cần setProductId vì nó được quản lý qua product
}