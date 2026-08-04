package com.poly.asm.daos;

import com.poly.asm.entitys.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Lấy đơn hàng có trạng thái là "PENDING"
    List<Order> findByStatus(String status);

    // Thay đổi query để truy vấn theo user và status
    List<Order> findByUser_IdAndStatus(Integer userId, String status);

    List<Order> findByUser_Id(Integer userId);

    // Lấy đơn hàng kèm chi tiết
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.variant v LEFT JOIN FETCH v.product p WHERE o.id = ?1")
    Order findByIdWithDetails(Long id);

    List<Order> findByUser_Id(Long userId);

}