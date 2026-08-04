package com.poly.asm.entitys;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt = new Date();

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CartItem> cartItems = new ArrayList<>(); // Khởi tạo mặc định là ArrayList rỗng

    // Constructor với user để khởi tạo giỏ hàng
    public Cart(User user) {
        this.user = user;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.cartItems = new ArrayList<>(); // Khởi tạo cartItems
    }

    // Phương thức cập nhật updatedAt trước khi lưu
    @PreUpdate
    @PrePersist
    public void updateTimestamps() {
        this.updatedAt = new Date();
    }
}