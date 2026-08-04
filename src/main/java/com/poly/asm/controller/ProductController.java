package com.poly.asm.controller;

import com.poly.asm.daos.*;
import com.poly.asm.entitys.*;
import com.poly.asm.services.CartService;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/product")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @ModelAttribute
    public void addAttributesToAllModels(HttpServletRequest request, Model model) {
        Cart cart = cartService.getCart(request);
        int cartItemCount = cart != null ? cart.getCartItems().size() : 0;
        model.addAttribute("cartItemCount", cartItemCount);
    }
    // Xem san pham
    @GetMapping
    public String showAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String variantSize,
            @RequestParam(required = false) String color,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage;
        boolean isFiltered = (variantSize != null && !variantSize.isEmpty()) || (color != null && !color.isEmpty());
        if (isFiltered) {
            productPage = productRepository.findByVariantAttributes(variantSize, color, pageable);
            logger.info("Filtered products - Page: {}, Total elements: {}, Content size: {}, VariantSize: {}, Color: {}", 
                    page, productPage.getTotalElements(), productPage.getContent().size(), variantSize, color);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        logger.info("Show all products - Page: {}, Total elements: {}, VariantSize: {}, Color: {}", page, productPage.getTotalElements(), variantSize, color);
        List<Product> products = productPage.getContent();

        // Tạo imageUrls
        Map<Long, String> imageUrls = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> productImageRepository.findPrimaryImageByProductId(p.getId())
                                .map(ProductImage::getImageUrl)
                                .orElse("/path/to/product.jpg"),
                        (existing, newValue) -> existing
                ));

        // Tạo formattedPrices
        DecimalFormat df = new DecimalFormat("#,##0");
        Map<Long, String> formattedPrices = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> {
                            List<ProductVariant> variants = productVariantRepository.findByProductId(p.getId());
                            if (variants.isEmpty()) {
                                return "COMING SOON";
                            }
                            BigDecimal minPrice = variants.stream()
                                    .map(ProductVariant::getPrice)
                                    .filter(v -> v != null)
                                    .min(BigDecimal::compareTo)
                                    .orElse(BigDecimal.ZERO);
                            return df.format(minPrice);
                        },
                        (existing, newValue) -> existing
                ));

        // Lấy danh sách size và màu sắc duy nhất
        List<String> sizes = productVariantRepository.findAll().stream()
                .filter(v -> v.getSize() != null)
                .map(ProductVariant::getSize)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> colors = productVariantRepository.findAll().stream()
                .filter(v -> v.getColor() != null)
                .map(ProductVariant::getColor)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("imageUrls", imageUrls);
        model.addAttribute("formattedPrices", formattedPrices);
        model.addAttribute("selectedCategory", null);
        model.addAttribute("selectedCategoryName", "Tất cả");
        model.addAttribute("variantSize", variantSize);
        model.addAttribute("color", color);
        model.addAttribute("sizes", sizes);
        model.addAttribute("colors", colors);
        model.addAttribute("isFiltered", isFiltered);

        return "web/product";
    }
    
    //Loc san pham theo danh muc
    @GetMapping("/category/{id}")
    public String showProductsByCategory(
            @PathVariable("id") Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String variantSize,
            @RequestParam(required = false) String color,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage;
        logger.info("Processing category {} with variantSize: {}, color: {}", categoryId, variantSize, color);
        if ((variantSize != null && !variantSize.isEmpty()) || (color != null && !color.isEmpty())) {
            productPage = productRepository.findByCategoryIdAndVariantAttributes(categoryId, variantSize, color, pageable);
            logger.info("Using variant filter - Total elements: {}, Content size: {}", productPage.getTotalElements(), productPage.getContent().size());
        } else {
            productPage = productRepository.findByCategoryId(categoryId, pageable);
            logger.info("Using category only - Total elements: {}, Content size: {}", productPage.getTotalElements(), productPage.getContent().size());
        }
        List<Product> products = productPage.getContent();

        // Tạo imageUrls
        Map<Long, String> imageUrls = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> productImageRepository.findPrimaryImageByProductId(p.getId())
                                .map(ProductImage::getImageUrl)
                                .orElse("/path/to/product.jpg"),
                        (existing, newValue) -> existing
                ));

        // Tạo formattedPrices
        DecimalFormat df = new DecimalFormat("#,##0");
        Map<Long, String> formattedPrices = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> {
                            List<ProductVariant> variants = productVariantRepository.findByProductId(p.getId());
                            if (variants.isEmpty()) {
                                return "COMING SOON";
                            }
                            BigDecimal minPrice = variants.stream()
                                    .map(ProductVariant::getPrice)
                                    .filter(Objects::nonNull)
                                    .min(BigDecimal::compareTo)
                                    .orElse(BigDecimal.ZERO);
                            return df.format(minPrice);
                        },
                        (existing, newValue) -> existing
                ));

        // Lấy danh sách size và màu sắc duy nhất từ danh mục
        List<String> sizes = productVariantRepository.findByProductCategoryId(categoryId).stream()
                .filter(v -> v.getSize() != null)
                .map(ProductVariant::getSize)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> colors = productVariantRepository.findByProductCategoryId(categoryId).stream()
                .filter(v -> v.getColor() != null)
                .map(ProductVariant::getColor)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        String selectedCategoryName = categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("Tất cả");

        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("imageUrls", imageUrls);
        model.addAttribute("formattedPrices", formattedPrices);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("selectedCategoryName", selectedCategoryName);
        model.addAttribute("variantSize", variantSize);
        model.addAttribute("color", color);
        model.addAttribute("sizes", sizes);
        model.addAttribute("colors", colors);
        model.addAttribute("isFiltered", (variantSize != null && !variantSize.isEmpty()) || (color != null && !color.isEmpty()));

        return "web/product";
    }
    
    //Xem chi tiet san pham
    @GetMapping("/detail/{id}")
    public String viewProductDetail(@PathVariable("id") Long id, Model model) {
        Product product = productRepository.findByIdWithPrimaryImages(id);
        if (product == null) {
            logger.warn("Product not found for ID: {}", id);
            return "redirect:/product";
        }

        List<ProductImage> images = productImageRepository.findByProductId(id);
        List<ProductVariant> variants = productVariantRepository.findByProductId(id);

        List<String> sizes = variants.stream()
                .filter(v -> v != null && v.getSize() != null)
                .map(ProductVariant::getSize)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> colors = variants.stream()
                .filter(v -> v != null && v.getColor() != null)
                .map(ProductVariant::getColor)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        logger.info("Product ID: {}, Images: {}, Variants: {}, Sizes: {}, Colors: {}",
                id, images.size(), variants.size(), sizes, colors);

        List<Product> relatedProducts = productRepository.findByCategoryIdAndIdNot(
                product.getCategory().getId(), id, PageRequest.of(0, 6)).getContent();

        Map<Long, String> relatedProductImages = relatedProducts.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> productImageRepository.findPrimaryImageByProductId(p.getId())
                                .map(ProductImage::getImageUrl)
                                .orElse("/path/to/product.jpg"),
                        (existing, newValue) -> existing
                ));

        DecimalFormat df = new DecimalFormat("#,##0");
        Map<Long, String> relatedProductPrices = relatedProducts.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> {
                            List<ProductVariant> relVariants = productVariantRepository.findByProductId(p.getId());
                            if (relVariants.isEmpty()) {
                                return "COMING SOON";
                            }
                            BigDecimal minPrice = relVariants.stream()
                                    .map(ProductVariant::getPrice)
                                    .filter(v -> v != null)
                                    .min(BigDecimal::compareTo)
                                    .orElse(BigDecimal.ZERO);
                            return df.format(minPrice);
                        },
                        (existing, newValue) -> existing
                ));

        model.addAttribute("product", product);
        model.addAttribute("images", images);
        model.addAttribute("variants", variants);
        model.addAttribute("sizes", sizes);
        model.addAttribute("colors", colors);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("relatedProductImages", relatedProductImages);
        model.addAttribute("relatedProductPrices", relatedProductPrices);

        return "web/productdetail";
    }

    //Tim kiem san pham
    @GetMapping("/search")
    public String searchProducts(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String variantSize,
            @RequestParam(required = false) String color,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage;
        if (variantSize != null || color != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndVariantAttributes(keyword, variantSize, color, pageable);
        } else {
            productPage = productRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }
        logger.info("Search products - Keyword: {}, Page: {}, Total elements: {}, VariantSize: {}, Color: {}", keyword, page, productPage.getTotalElements(), variantSize, color);
        List<Product> products = productPage.getContent();

        Map<Long, String> imageUrls = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> productImageRepository.findPrimaryImageByProductId(p.getId())
                                .map(ProductImage::getImageUrl)
                                .orElse("/path/to/product.jpg"),
                        (existing, newValue) -> existing
                ));

        DecimalFormat df = new DecimalFormat("#,##0");
        Map<Long, String> formattedPrices = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> {
                            List<ProductVariant> variants = productVariantRepository.findByProductId(p.getId());
                            if (variants.isEmpty()) {
                                return "COMING SOON";
                            }
                            BigDecimal minPrice = variants.stream()
                                    .map(ProductVariant::getPrice)
                                    .filter(v -> v != null)
                                    .min(BigDecimal::compareTo)
                                    .orElse(BigDecimal.ZERO);
                            return df.format(minPrice);
                        },
                        (existing, newValue) -> existing
                ));

        List<String> sizes = productVariantRepository.findAll().stream()
                .filter(v -> v.getSize() != null)
                .map(ProductVariant::getSize)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> colors = productVariantRepository.findAll().stream()
                .filter(v -> v.getColor() != null)
                .map(ProductVariant::getColor)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("imageUrls", imageUrls);
        model.addAttribute("formattedPrices", formattedPrices);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", null);
        model.addAttribute("selectedCategoryName", "Tất cả");
        model.addAttribute("variantSize", variantSize);
        model.addAttribute("color", color);
        model.addAttribute("sizes", sizes);
        model.addAttribute("colors", colors);

        return "web/product";
    }
}