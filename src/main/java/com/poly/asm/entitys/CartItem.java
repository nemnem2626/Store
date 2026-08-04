package com.poly.asm.entitys;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@Table(name = "CartItems")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnoreProperties({"product", "cartItems"})
    private ProductVariant variant;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    private String size;
    private String color;

    // Constructor với các trường cần thiết
    public CartItem(Cart cart, ProductVariant variant, String productName, BigDecimal price, Integer quantity) {
        this.cart = cart;
        this.variant = variant;
        this.productName = productName != null ? productName : (variant != null ? variant.getProduct().getName() : "");
        this.price = price != null ? price : (variant != null ? variant.getPrice() : BigDecimal.ZERO);
        this.quantity = quantity != null ? quantity : 0;
        if (variant != null) {
            this.size = variant.getSize();
            this.color = variant.getColor();
        }
    }

    // Tính giá trị tổng của một CartItem (price * quantity)
    public BigDecimal getTotalPrice() {
        return price != null && quantity != null ? price.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }
}