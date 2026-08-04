package com.poly.asm.daos;

import com.poly.asm.entitys.Product;
import com.poly.asm.entitys.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId); // Lấy tất cả variant theo sản phẩm
    
    void deleteByProductId(Long productId);
   

    @Query("SELECT v FROM ProductVariant v WHERE v.product.category.id = :categoryId")
    List<ProductVariant> findByProductCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT pv.product FROM ProductVariant pv WHERE pv.price <= :discountPrice ORDER BY pv.price ASC")
    List<Product> findTop10ProductByVariantPriceLessThanEqual(@Param("discountPrice") BigDecimal discountPrice); // Đổi double thành BigDecimal

    @Query("SELECT DISTINCT pv.product FROM ProductVariant pv " +
           "WHERE LOWER(pv.product.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND pv.product.category.id = :categoryId " +
           "AND pv.price BETWEEN :minPrice AND :maxPrice")
    List<Product> searchByNameCategoryAndPrice(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice); // Đổi double thành BigDecimal
    
    Optional<ProductVariant> findByProductAndSizeAndColor(Product product, String size, String color);
}