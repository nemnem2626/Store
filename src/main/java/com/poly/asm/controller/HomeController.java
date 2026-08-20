package com.poly.asm.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.poly.asm.daos.CategoryRepository;
import com.poly.asm.daos.ProductImageRepository;
import com.poly.asm.daos.ProductRepository;
import com.poly.asm.daos.ProductVariantRepository;
import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.Product;
import com.poly.asm.entitys.ProductImage;
import com.poly.asm.entitys.ProductVariant;
import com.poly.asm.services.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

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
    
    @GetMapping("/home")
    public String showHomePage(Model model, HttpServletRequest request) {
        // Lấy 10 sản phẩm mới nhất, sắp xếp theo id giảm dần
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> newProductsPage = productRepository.findAll(pageable);
        List<Product> newProducts = newProductsPage.getContent();

        // Tạo map imageUrls - lấy ảnh đại diện, nếu không có thì lấy ảnh đầu tiên
        Map<Long, String> imageUrls = newProducts.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> {
                            // Cố gắng lấy ảnh đại diện (variant IS NULL)
                            Optional<String> primaryImage = productImageRepository.findPrimaryImageByProductId(p.getId())
                                    .map(ProductImage::getImageUrl);
                            
                            // Nếu không có, lấy ảnh đầu tiên của sản phẩm
                            if (primaryImage.isPresent()) {
                                return primaryImage.get();
                            }
                            
                            List<ProductImage> allImages = productImageRepository.findByProductId(p.getId());
                            if (!allImages.isEmpty()) {
                                return allImages.get(0).getImageUrl();
                            }
                            
                            return "/images/no-image.svg";
                        },
                        (existing, newValue) -> existing
                ));

        // Tạo map formattedPrices
        DecimalFormat df = new DecimalFormat("#,##0");
        Map<Long, String> formattedPrices = newProducts.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> {
                            List<ProductVariant> variants = productVariantRepository.findByProductId(p.getId());
                            if (variants.isEmpty()) {
                                return "0";
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

        // Thêm dữ liệu vào model
        model.addAttribute("newProducts", newProducts);
        model.addAttribute("imageUrls", imageUrls);
        model.addAttribute("formattedPrices", formattedPrices);
        model.addAttribute("categories", categoryRepository.findAll());

        // Debug logging
        logger.info("Home page: {} products loaded", newProducts.size());
        newProducts.forEach(p -> {
            String imageUrl = imageUrls.get(p.getId());
            logger.info("Product ID={}, Name={}, Image={}", p.getId(), p.getName(), imageUrl);
        });

        return "web/home";
    }
    
    @GetMapping("/introduction")
    public String introduction() {
        return "web/introduction";
    }
    
    @GetMapping("/policy")
    public String policy() {
        return "web/policy";
    }
}
