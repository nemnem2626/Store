package com.poly.asm.daos;

import com.poly.asm.entitys.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    // Lấy giỏ hàng theo user_id
    Optional<Cart> findByUserId(Integer userId);
}