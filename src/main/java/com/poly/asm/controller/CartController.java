package com.poly.asm.controller;

import com.poly.asm.daos.OrderDetailRepository;
import com.poly.asm.daos.OrderRepository;
import com.poly.asm.daos.ProductImageRepository;
import com.poly.asm.daos.ProductVariantRepository;
import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.CartItem;
import com.poly.asm.entitys.Order;
import com.poly.asm.entitys.OrderDetail;
import com.poly.asm.entitys.ProductVariant;
import com.poly.asm.entitys.User;
import com.poly.asm.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @PostConstruct
    public void init() {
        logger.info("CartController initialized. Registered endpoints: /cart/*");
    }

    @ModelAttribute
    public void addAttributesToAllModels(HttpServletRequest request, Model model) {
        Cart cart = cartService.getCart(request);
        int cartItemCount = cart != null ? cart.getCartItems().size() : 0;
        model.addAttribute("cartItemCount", cartItemCount);
    }

    @GetMapping
    public String viewCart(HttpServletRequest request, Model model) {
        if (request.getSession().getAttribute("user") == null) {
            return "redirect:/login";
        }

        Cart cart = cartService.getCart(request);
        double total = cartService.getTotalPrice(cart);

        Map<Long, String> imageUrls = new HashMap<>();
        Map<Long, String> sizes = new HashMap<>();
        Map<Long, String> colors = new HashMap<>();
        Map<Long, Integer> stocks = new HashMap<>();
        if (cart != null) {
            for (CartItem item : cart.getCartItems()) {
                Long variantId = item.getVariant().getId();
                String imageUrl = productImageRepository.findPrimaryImageByProductId(item.getVariant().getProduct().getId())
                        .map(img -> img.getImageUrl())
                        .orElse("path/to/placeholder.jpg");
                imageUrls.put(variantId, imageUrl);
                sizes.put(variantId, item.getVariant().getSize() != null ? item.getVariant().getSize() : "N/A");
                colors.put(variantId, item.getVariant().getColor() != null ? item.getVariant().getColor() : "N/A");
                stocks.put(variantId, item.getVariant().getStock());
            }
        }

        List<CartItem> cartItems = cart != null ? cart.getCartItems() : new ArrayList<>();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("imageUrls", imageUrls);
        model.addAttribute("sizes", sizes);
        model.addAttribute("colors", colors);
        model.addAttribute("stocks", stocks);
        model.addAttribute("cartTotal", total);

        logger.info("Accessing /cart for user: {}, cartId: {}, items: {}", 
                    request.getSession().getAttribute("user") != null ? ((User)request.getSession().getAttribute("user")).getUsername() : "null",
                    cart != null ? cart.getId() : "null",
                    cart != null ? cart.getCartItems().size() : 0);

        return "web/cart";
    }

    @PostMapping("/add/{variantId}")
    public String addToCart(@PathVariable Long variantId,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           RedirectAttributes redirectAttributes) {
        if (request.getSession().getAttribute("user") == null) {
            return "redirect:/login";
        }
        String error = cartService.addToCart(request, response, variantId, 1);
        if (error != null) {
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            if (variant != null) {
                return "redirect:/product/detail/" + variant.getProduct().getId() + "?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
            }
        }
        redirectAttributes.addFlashAttribute("success", "Thêm vào giỏ hàng thành công!");
        return "redirect:/cart";
    }

    @PostMapping("/add")
    public String addToCartFromForm(
            @RequestParam("productId") Long productId,
            @RequestParam("variantId") Long variantId,
            @RequestParam("quantity") int quantity,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!");
            return "redirect:/login";
        }

        String error = cartService.addToCart(request, response, variantId, quantity);
        if (error != null) {
            return "redirect:/product/detail/" + productId + "?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
        }
        redirectAttributes.addFlashAttribute("success", "Thêm vào giỏ hàng thành công!");
        return "redirect:/product/detail/" + productId;
    }

    @PostMapping("/update/{variantId}")
    public String updateCart(@PathVariable Long variantId,
                            @RequestParam int quantity,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            RedirectAttributes redirectAttributes) {
        if (request.getSession().getAttribute("user") == null) {
            return "redirect:/login";
        }
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null) {
            redirectAttributes.addFlashAttribute("error", "Biến thể không tồn tại!");
            return "redirect:/cart";
        }

        String error = cartService.updateCart(request, variantId, quantity);
        if (error != null) {
            return "redirect:/cart?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
        }
        redirectAttributes.addFlashAttribute("success", "Cập nhật giỏ hàng thành công!");
        return "redirect:/cart";
    }

    @PostMapping("/remove/{variantId}")
    public String removeFromCart(@PathVariable Long variantId,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        if (request.getSession().getAttribute("user") == null) {
            return "redirect:/login";
        }
        cartService.removeFromCart(request, variantId);
        redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công!");
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkout(HttpServletRequest request, Model model) {
        if (request.getSession().getAttribute("user") == null) {
            return "redirect:/login";
        }

        Cart cart = cartService.getCart(request);
        if (cart == null || cart.getCartItems().isEmpty()) {
            model.addAttribute("error", "Giỏ hàng của bạn trống!");
            return "redirect:/cart";
        }

        double total = cartService.getTotalPrice(cart);
        User user = (User) request.getSession().getAttribute("user");

        model.addAttribute("cartItems", cart.getCartItems());
        model.addAttribute("cartTotal", total);
        model.addAttribute("user", user);

        logger.info("Accessing /cart/checkout for user: {}, cartId: {}, items: {}", 
                    user != null ? user.getUsername() : "null", 
                    cart != null ? cart.getId() : "null", 
                    cart != null ? cart.getCartItems().size() : 0);

        return "web/checkout";
    }

    @PostMapping("/process-payment")
    public String processPayment(@RequestParam String address,
                                @RequestParam String fullname,
                                @RequestParam String phone,
                                @RequestParam String paymentMethod,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) {
        if (request.getSession().getAttribute("user") == null) {
            return "redirect:/login";
        }

        Cart cart = cartService.getCart(request);
        if (cart == null || cart.getCartItems().isEmpty()) {
            model.addAttribute("error", "Giỏ hàng trống, không thể thanh toán!");
            return "redirect:/cart/checkout";
        }

        // Kiểm tra tính hợp lệ của dữ liệu
        if (fullname == null || fullname.trim().isEmpty()) {
            model.addAttribute("error", "Họ và tên không được để trống!");
            return "redirect:/cart/checkout";
        }
        if (!fullname.matches("^[\\p{L}\\s]+$")) {
            model.addAttribute("error", "Họ và tên chỉ được chứa chữ cái và khoảng trắng!");
            return "redirect:/cart/checkout";
        }
        if (!phone.matches("^\\d{10}$")) {
            model.addAttribute("error", "Số điện thoại phải là 10 chữ số!");
            return "redirect:/cart/checkout";
        }
        if (address == null || address.trim().isEmpty()) {
            model.addAttribute("error", "Địa chỉ không được để trống!");
            return "redirect:/cart/checkout";
        }
        if (!paymentMethod.equals("COD") && !paymentMethod.equals("VNPAY")) {
            model.addAttribute("error", "Phương thức thanh toán không hợp lệ!");
            return "redirect:/cart/checkout";
        }

        // Kiểm tra tồn kho
        for (CartItem item : cart.getCartItems()) {
            ProductVariant variant = item.getVariant();
            if (variant.getStock() < item.getQuantity()) {
                model.addAttribute("error", "Số lượng sản phẩm '" + variant.getProduct().getName() + "' vượt quá tồn kho (" + variant.getStock() + ")!");
                return "redirect:/cart/checkout";
            }
        }

        double total = cartService.getTotalPrice(cart);
        User user = (User) request.getSession().getAttribute("user");

        if ("VNPAY".equals(paymentMethod)) {
            // Lưu thông tin từ form vào session để VNPAYController sử dụng
            request.getSession().setAttribute("order_fullname", fullname);
            request.getSession().setAttribute("order_phone", phone);
            request.getSession().setAttribute("order_address", address);

            String vnpayUrl = createVNPayPaymentUrl(request, total, "Thanh toán đơn hàng");
            return "redirect:" + vnpayUrl;
        } else {
            Order order = new Order();
            order.setFullname(fullname);
            order.setPhone(phone);
            order.setAddress(address);
            order.setPaymentMethod(paymentMethod);
            order.setTotalPrice(cartService.getTotalPrice(cart));
            order.setUser(user);
            order.setStatus("PENDING");

            try {
                order = orderRepository.save(order);

                for (CartItem cartItem : cart.getCartItems()) {
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setOrder(order);
                    orderDetail.setVariant(cartItem.getVariant());
                    orderDetail.setQuantity(cartItem.getQuantity());
                    orderDetail.setPrice(cartItem.getPrice());
                    orderDetailRepository.save(orderDetail);

                    ProductVariant variant = cartItem.getVariant();
                    variant.setStock(variant.getStock() - cartItem.getQuantity());
                    productVariantRepository.save(variant);
                }

                cartService.clearCart(request);
                return "redirect:/cart/success";
            } catch (Exception e) {
                logger.error("Lỗi khi xử lý đơn hàng COD: {}", e.getMessage(), e);
                model.addAttribute("error", "Đã xảy ra lỗi khi xử lý đơn hàng. Vui lòng thử lại!");
                return "redirect:/cart/checkout";
            }
        }
    }

    private String createVNPayPaymentUrl(HttpServletRequest request, double amount, String orderInfo) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPAYConfig.VNP_VERSION);
        vnp_Params.put("vnp_Command", VNPAYConfig.VNP_COMMAND);
        vnp_Params.put("vnp_TmnCode", VNPAYConfig.VNP_TMN_CODE);
        vnp_Params.put("vnp_Amount", String.valueOf((long) (amount * 100)));
        vnp_Params.put("vnp_CurrCode", VNPAYConfig.VNP_CURR_CODE);
        vnp_Params.put("vnp_TxnRef", String.valueOf(System.currentTimeMillis()));
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", VNPAYConfig.DEFAULT_LOCALE);
        vnp_Params.put("vnp_ReturnUrl", VNPAYConfig.VNP_RETURN_URL);
        vnp_Params.put("vnp_IpAddr", getIpAddress(request));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(new Date());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                     .append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        logger.info("Tạo HashData: {}", hashData.toString());
        String vnp_SecureHash = hmacSHA512(VNPAYConfig.VNP_HASH_SECRET, hashData.toString());
        logger.info("Tạo vnp_SecureHash: {}", vnp_SecureHash);

        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        String paymentUrl = VNPAYConfig.VNP_PAY_URL + "?" + query;
        logger.info("URL thanh toán: {}", paymentUrl);
        return paymentUrl;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error while generating HMAC SHA512", e);
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    @GetMapping("/success")
    public String success(HttpServletRequest request, Model model) {
        if (request.getSession().getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "cart/success";
    }
}