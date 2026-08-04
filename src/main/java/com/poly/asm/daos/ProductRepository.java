package com.poly.asm.daos;

import com.poly.asm.entitys.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Product> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Product findByIdWithPrimaryImages(@Param("id") Long id);
    
    Page<Product> findByCategoryIdAndIdNot(Long categoryId, Long id, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE (:variantSize IS NULL OR v.size = :variantSize) OR (:color IS NULL OR v.color = :color)")
    Page<Product> findByVariantAttributes(@Param("variantSize") String variantSize, @Param("color") String color, Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN p.variants v WHERE p.category.id = :categoryId AND " +
    	       "((:variantSize IS NULL OR v.size = :variantSize) OR (:color IS NULL OR v.color = :color) OR v.id IS NULL)")
    	Page<Product> findByCategoryIdAndVariantAttributes(@Param("categoryId") Long categoryId, 
    	                                                  @Param("variantSize") String variantSize, 
    	                                                  @Param("color") String color, 
    	                                                  Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND ( (:variantSize IS NULL OR v.size = :variantSize) OR (:color IS NULL OR v.color = :color) )")
    Page<Product> findByNameContainingIgnoreCaseAndVariantAttributes(@Param("keyword") String keyword, @Param("variantSize") String variantSize, @Param("color") String color, Pageable pageable);
}