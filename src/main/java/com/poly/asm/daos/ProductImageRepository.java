package com.poly.asm.daos;

import com.poly.asm.entitys.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId); // Lấy tất cả ảnh theo sản phẩm

    // Lấy ảnh theo variant_id
    List<ProductImage> findByVariantId(Long variantId);
    
    void deleteByVariantId(Long variantId);
    
    void deleteByProductId(Long productId);

    // Lấy ảnh đại diện (variant_id = NULL) cho sản phẩm
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.variant IS NULL ORDER BY pi.id")
    Optional<ProductImage> findPrimaryImageByProductId(@Param("productId") Long productId);
}