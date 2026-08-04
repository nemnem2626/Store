package com.poly.asm.entitys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.List;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "Orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullname;

    @Column(nullable = false, length = 10)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    private String status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "order_date")
    private java.util.Date orderDate = new java.util.Date();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<OrderDetail> orderDetails;
}