package com.poly.asm.controller;

import com.poly.asm.daos.*;
import com.poly.asm.entitys.*;
import com.poly.asm.services.OrderService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    // ----------- QUẢN LÝ ĐƠN HÀNG -----------

    // Danh sách đơn hàng
    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderRepository.findAll());
        return "admin/orders";
    }

    // Xem chi tiết đơn hàng
    @GetMapping("/orders/{id}")
    public String orderDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findByIdWithDetails(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/admin/orders";
        }
        model.addAttribute("order", order);
        return "admin/order-detail";
    }

    // Cập nhật trạng thái đơn hàng
    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam("status") String status,
                                   RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Trạng thái đơn hàng đã được cập nhật!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // Xóa đơn hàng
    @GetMapping("/orders/delete/{id}")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findByIdWithDetails(id);
        if (order == null || !"PENDING".equalsIgnoreCase(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể xóa đơn hàng ở trạng thái Chờ xử lý");
            return "redirect:/admin/orders";
        }
        try {
            orderService.updateOrderStatus(id, "CANCELED"); // Hoàn kho trước khi xóa
            orderRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đơn hàng đã được xóa!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // ----------- TRANG CHỦ -----------

    @GetMapping("/index")
    public String home() {
        return "admin/index";
    }

    // ----------- QUẢN LÝ SẢN PHẨM -----------

    @GetMapping("/products")
    public String listProducts(Model model) {
        List<Product> products = productRepository.findAll();
        Map<String, ProductGroup> productGroups = new HashMap<>(); // Gộp sản phẩm theo tên
        Map<Long, String> imageUrls = new HashMap<>(); // Lưu ảnh đại diện

        logger.info("Fetching all products, total count: {}", products.size());
        for (Product p : products) {
            String productName = p.getName();
            ProductGroup group = productGroups.computeIfAbsent(productName, k -> new ProductGroup());
            group.getProducts().add(p);

            // Lấy ảnh đại diện (chỉ lấy ảnh đầu tiên cho mỗi nhóm)
            if (imageUrls.get(p.getId()) == null) {
                String imageUrl = productImageRepository.findByProductId(p.getId())
                        .stream().filter(img -> img.getImageUrl() != null && !img.getImageUrl().isEmpty())
                        .findFirst()
                        .map(ProductImage::getImageUrl)
                        .orElse("/images/default.jpg");
                imageUrls.put(p.getId(), imageUrl);
                logger.debug("Assigned image URL {} for product ID: {}", imageUrl, p.getId());
            }
        }

        logger.info("Prepared {} product groups", productGroups.size());
        model.addAttribute("productGroups", productGroups.values());
        model.addAttribute("imageUrls", imageUrls); // Truyền imageUrls để dùng trong template
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("product", new Product()); // Đảm bảo product mới cho form
        return "admin/products";
    }

    // Lớp phụ để gộp sản phẩm và biến thể
    class ProductGroup {
        private List<Product> products = new ArrayList<>();
        private List<ProductVariant> variants = new ArrayList<>();

        public List<Product> getProducts() {
            return products;
        }

        public List<ProductVariant> getVariants() {
            return variants;
        }
    }

    @PostMapping("/products/save")
    @Transactional
    public String saveProduct(@ModelAttribute("product") Product product,
                             @RequestParam("imageFiles") MultipartFile[] imageFiles,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            // Lưu sản phẩm
            Product savedProduct = productRepository.save(product);
            logger.info("Saved product with ID: {}", savedProduct.getId());

            // Xử lý ảnh chung (ảnh chính)
            String uploadDir = request.getServletContext().getRealPath("uploads/");
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            if (imageFiles != null) {
                productImageRepository.deleteByProductId(savedProduct.getId()); // Xóa ảnh cũ
                for (MultipartFile imageFile : imageFiles) {
                    if (!imageFile.isEmpty()) {
                        String originalFileName = imageFile.getOriginalFilename();
                        String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
                        File saveFile = new File(uploadDir + sanitizedFileName);
                        imageFile.transferTo(saveFile);

                        ProductImage productImage = new ProductImage();
                        productImage.setProduct(savedProduct);
                        productImage.setImageUrl("/uploads/" + sanitizedFileName);
                        productImageRepository.save(productImage);
                        logger.info("Saved product image: {}", sanitizedFileName);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được lưu thành công!");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        // Lấy ảnh đại diện
        Map<Long, String> imageUrls = new HashMap<>();
        String imageUrl = productImageRepository.findByProductId(id)
                .stream().filter(img -> img.getImageUrl() != null && !img.getImageUrl().isEmpty())
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse("/images/default.jpg");
        imageUrls.put(product.getId(), imageUrl);

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("imageUrls", imageUrls); // Truyền imageUrls cho form
        return "admin/products";
    }

    @GetMapping("/products/delete/{id}")
    @Transactional
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (productRepository.existsById(id)) {
            logger.info("Starting deletion process for product ID: {}", id);

            List<ProductVariant> variants = productVariantRepository.findByProductId(id);
            logger.info("Found {} variants for product ID: {}", variants.size(), id);

            for (ProductVariant variant : variants) {
                if (variant != null && variant.getId() != null) {
                    Long variantId = variant.getId();
                    logger.info("Checking CartItems for variant ID: {}", variantId);

                    List<CartItem> cartItems = cartItemRepository.findByVariantId(variantId);
                    logger.info("Found {} CartItems for variant ID: {}", cartItems.size(), variantId);
                    if (!cartItems.isEmpty()) {
                        logger.warn("Cannot delete product ID: {} because variant ID: {} is in cart with {} items", id, variantId, cartItems.size());
                        redirectAttributes.addFlashAttribute("error", "Không thể xóa sản phẩm vì nó vẫn đang trong giỏ hàng của người dùng!");
                        logger.debug("Flash attribute 'error' set to: {}", redirectAttributes.getFlashAttributes().get("error"));
                        return "redirect:/admin/products";
                    }
                }
            }

            // Tiếp tục xóa nếu không có CartItems
            for (ProductVariant variant : variants) {
                if (variant != null && variant.getId() != null) {
                    Long variantId = variant.getId();
                    orderDetailRepository.deleteByVariantId(variantId);
                    logger.info("Deleted OrderDetails for variant ID: {}", variantId);
                }
            }
            productImageRepository.deleteByProductId(id);
            logger.info("Deleted ProductImages for product ID: {}", id);
            productVariantRepository.deleteByProductId(id);
            logger.info("Deleted ProductVariants for product ID: {}", id);
            productRepository.deleteById(id);
            logger.info("Deleted Product for ID: {}", id);
            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được xóa!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
        }
        return "redirect:/admin/products";
    }

    // ----------- QUẢN LÝ BIẾN THỂ -----------

    @GetMapping("/variants")
    public String listVariants(Model model) {
        List<ProductVariant> variants = productVariantRepository.findAll();
        logger.info("Fetched {} variants", variants.size());

        // Kiểm tra và gán product cho mỗi variant nếu bị null
        for (ProductVariant variant : variants) {
            if (variant.getProduct() == null && variant.getProductId() != null) {
                Product product = productRepository.findById(variant.getProductId()).orElse(null);
                if (product != null) {
                    variant.setProduct(product);
                    logger.debug("Assigned product {} to variant ID: {}", product.getName(), variant.getId());
                } else {
                    logger.warn("No product found for variant ID: {} with product_id: {}", variant.getId(), variant.getProductId());
                }
            }
        }

        model.addAttribute("variants", variants);
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("variant", new ProductVariant());
        return "admin/variants";
    }

    @GetMapping("/variants/edit/{id}")
    public String editVariant(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ProductVariant variant = productVariantRepository.findById(id).orElse(null);
        if (variant == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể!");
            return "redirect:/admin/variants";
        }
        model.addAttribute("variant", variant);
        model.addAttribute("products", productRepository.findAll());
        return "admin/variants";
    }

    @PostMapping("/variants/save")
    @Transactional
    public String saveVariant(@ModelAttribute("variant") ProductVariant variant,
                             @RequestParam("imageFiles") MultipartFile[] imageFiles,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            Product product = productRepository.findById(variant.getProduct().getId()).orElse(null);
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại!");
                return "redirect:/admin/variants";
            }
            variant.setProduct(product);
            ProductVariant savedVariant = productVariantRepository.save(variant);
            logger.info("Saved variant with ID: {}", savedVariant.getId());

            String uploadDir = request.getServletContext().getRealPath("/uploads/");
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            if (imageFiles != null) {
                productImageRepository.deleteByVariantId(savedVariant.getId());
                for (MultipartFile imageFile : imageFiles) {
                    if (!imageFile.isEmpty()) {
                        String originalFileName = imageFile.getOriginalFilename();
                        String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
                        File saveFile = new File(uploadDir + sanitizedFileName);
                        imageFile.transferTo(saveFile);

                        ProductImage productImage = new ProductImage();
                        productImage.setProduct(product);
                        productImage.setVariant(savedVariant);
                        productImage.setImageUrl("/uploads/" + sanitizedFileName);
                        productImageRepository.save(productImage);
                        logger.info("Saved variant image: {}", sanitizedFileName);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success", "Biến thể đã được lưu thành công!");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
        }
        return "redirect:/admin/variants";
    }

    @GetMapping("/variants/delete/{id}")
    @Transactional
    public String deleteVariant(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ProductVariant variant = productVariantRepository.findById(id).orElse(null);
        if (variant == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể!");
            return "redirect:/admin/variants";
        }

        logger.info("Starting deletion process for variant ID: {}", id);

        // Kiểm tra nếu variant đang trong giỏ hàng
        List<CartItem> cartItems = cartItemRepository.findByVariantId(id);
        if (!cartItems.isEmpty()) {
            logger.warn("Cannot delete variant ID: {} because it is in cart with {} items", id, cartItems.size());
            redirectAttributes.addFlashAttribute("error", "Không thể xóa biến thể vì nó vẫn đang trong giỏ hàng của người dùng!");
            return "redirect:/admin/variants";
        }

        // Xóa OrderDetails liên quan
        orderDetailRepository.deleteByVariantId(id);
        logger.info("Deleted OrderDetails for variant ID: {}", id);

        // Xóa ảnh liên quan
        productImageRepository.deleteByVariantId(id);
        logger.info("Deleted ProductImages for variant ID: {}", id);

        // Xóa variant
        productVariantRepository.deleteById(id);
        logger.info("Deleted Variant for ID: {}", id);

        redirectAttributes.addFlashAttribute("success", "Biến thể đã được xóa!");
        return "redirect:/admin/variants";
    }

    // ----------- QUẢN LÝ DANH MỤC -----------

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("category", new Category());
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Danh mục đã được lưu thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (!categoryOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", categoryOpt.get());
        return "admin/categories";
    }

    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable Long id, @ModelAttribute Category category,
                                RedirectAttributes redirectAttributes) {
        if (!categoryRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }
        category.setId(id);
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Danh mục đã được cập nhật!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra xem danh mục có tồn tại không
            if (!categoryRepository.existsById(id)) {
                throw new EntityNotFoundException("Không tìm thấy danh mục với ID: " + id);
            }

            // Kiểm tra số lượng sản phẩm liên kết
            Long productCount = categoryRepository.countProductsByCategoryId(id);
            if (productCount > 0) {
                redirectAttributes.addFlashAttribute("error", "Danh mục chứa sản phẩm, không thể xóa!");
                return "redirect:/admin/categories";
            }

            // Xóa danh mục nếu không có sản phẩm
            categoryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Danh mục đã được xóa!");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi xóa danh mục!");
        }
        return "redirect:/admin/categories";
    }

    // ----------- QUẢN LÝ NGƯỜI DÙNG -----------

    // Danh sách người dùng
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("user", new User());
        return "admin/users";
    }

    // Lưu người dùng (thêm mới)
    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
            }
            user.setProvider("form"); // Mặc định cho người dùng thêm mới
            user.setProviderId(null); // Không cần providerId cho form
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Người dùng đã được lưu thành công!");
        } catch (Exception e) {
            logger.error("Failed to save user: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // Chỉnh sửa người dùng
    @GetMapping("/users/edit/{id}")
    public String editUser(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", userOpt.get());
        return "admin/users";
    }

    // Cập nhật người dùng
    @PostMapping("/users/update/{id}")
    @Transactional
    public String updateUser(@PathVariable Integer id, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (!existingUserOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/admin/users";
        }

        User existingUser = existingUserOpt.get();
        try {
            // Giữ nguyên provider và providerId
            user.setProvider(existingUser.getProvider());
            user.setProviderId(existingUser.getProviderId());

            // Chỉ mã hóa mật khẩu nếu được cung cấp
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
            } else {
                user.setPassword(existingUser.getPassword()); // Giữ mật khẩu cũ
            }

            // Cập nhật các trường khác
            user.setId(id);
            userRepository.save(user);
            logger.info("Updated user with ID: {}, username: {}, role: {}", id, user.getUsername(), user.getRole());
            redirectAttributes.addFlashAttribute("success", "Thông tin người dùng đã được cập nhật!");
        } catch (Exception e) {
            logger.error("Failed to update user with ID: {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // Xóa người dùng
    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        if (userRepository.existsById(id)) {
            try {
                userRepository.deleteById(id);
                redirectAttributes.addFlashAttribute("success", "Người dùng đã được xóa!");
            } catch (DataIntegrityViolationException e) {
                redirectAttributes.addFlashAttribute("error", "Không thể xóa người dùng vì có giỏ hàng tồn tại. Vui lòng xóa giỏ hàng trước!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa người dùng: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng!");
        }
        return "redirect:/admin/users";
    }
}