package com.poly.asm.daos;

import com.poly.asm.entitys.Order;
import com.poly.asm.entitys.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    // Lấy danh sách chi tiết theo Order
    List<OrderDetail> findByOrder(Order order);

    // Thêm phương thức để lấy chi tiết theo variant_id
    List<OrderDetail> findByVariantId(Long variantId);
    
    void deleteByVariantId(Long variantId);
}