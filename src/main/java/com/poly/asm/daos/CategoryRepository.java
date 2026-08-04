package com.poly.asm.daos;

import com.poly.asm.entitys.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Lấy tất cả các loại sản phẩm
    List<Category> findAll();

    // Thêm phương thức tìm theo tên (tùy chọn)
    Category findByName(String name);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = ?1")
    Long countProductsByCategoryId(Long categoryId);
}
