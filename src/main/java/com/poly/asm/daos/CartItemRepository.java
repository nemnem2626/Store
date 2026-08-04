package com.poly.asm.daos;

import com.poly.asm.entitys.CartItem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByVariantId(Long variantId); // Phương thức để xóa CartItem theo variant_id
    List<CartItem> findByVariantId(Long variantId);
}