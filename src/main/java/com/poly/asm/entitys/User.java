package com.poly.asm.entitys;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullname;

    @Column(length = 10)
    private String phone;

    private String address;

    @Column(columnDefinition = "NVARCHAR(20) CHECK (role IN ('USER', 'ADMIN', 'STAFF')) DEFAULT 'USER'")
    private String role;

    private Boolean active;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String provider;
    
    @Column(columnDefinition = "NVARCHAR(255)")
    private String providerId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Cart> carts;
}