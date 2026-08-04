package com.poly.asm.services;

import com.poly.asm.controller.ErrorController;
import com.poly.asm.daos.CartRepository;
import com.poly.asm.daos.ProductVariantRepository;
import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.CartItem;
import com.poly.asm.entitys.ProductVariant;
import com.poly.asm.entitys.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {

	private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);
	
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    // Lấy giỏ hàng của user hiện tại
    public Cart getCart(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            logger.warn("Không tìm thấy user trong session");
            return null;
        }
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    logger.info("Tạo giỏ hàng mới cho user: {}", user.getUsername());
                    Cart newCart = new Cart(user);
                    return cartRepository.save(newCart);
                });
        logger.info("Lấy giỏ hàng cho user: {}, cartId: {}, số lượng items: {}", 
                    user.getUsername(), cart.getId(), cart.getCartItems().size());
        return cart;
    }

    // Thêm sản phẩm vào giỏ hàng
    public String addToCart(HttpServletRequest request, HttpServletResponse response, Long variantId, int quantity) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "Bạn cần đăng nhập để thêm sản phẩm vào giỏ hàng!";
        }

        Cart cart = getCart(request);
        Optional<ProductVariant> optionalVariant = productVariantRepository.findById(variantId);
        if (optionalVariant.isEmpty()) {
            return "Biến thể không tồn tại!";
        }

        ProductVariant variant = optionalVariant.get();
        if (variant.getStock() < quantity) {
            return "Số lượng yêu cầu (" + quantity + ") vượt quá tồn kho (" + variant.getStock() + ")!";
        }

        // Tìm CartItem hiện có
        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getVariant().getId().equals(variantId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            if (newQuantity <= variant.getStock()) {
                existingItem.setQuantity(newQuantity);
            } else {
                return "Số lượng tổng cộng (" + newQuantity + ") vượt quá tồn kho (" + variant.getStock() + ")!";
            }
        } else {
            CartItem newItem = new CartItem(cart, variant, variant.getProduct().getName(), variant.getPrice(), quantity);
            cart.getCartItems().add(newItem);
        }
        cartRepository.save(cart);
        return null; // Thành công
    }

    // Cập nhật số lượng
    public String updateCart(HttpServletRequest request, Long variantId, int quantity) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "Bạn cần đăng nhập để cập nhật giỏ hàng!";
        }

        Cart cart = getCart(request);
        Optional<CartItem> itemOpt = cart.getCartItems().stream()
                .filter(item -> item.getVariant().getId().equals(variantId))
                .findFirst();
        if (itemOpt.isEmpty()) {
            return "Sản phẩm không tồn tại trong giỏ hàng!";
        }

        CartItem item = itemOpt.get();
        ProductVariant variant = item.getVariant();
        if (quantity <= 0) {
            cart.getCartItems().remove(item);
            cartRepository.save(cart);
            return null;
        }
        if (quantity > variant.getStock()) {
            return "Số lượng yêu cầu (" + quantity + ") vượt quá tồn kho (" + variant.getStock() + ")!";
        }
        item.setQuantity(quantity);
        cartRepository.save(cart);
        return null;
    }

    // Xóa sản phẩm
    public void removeFromCart(HttpServletRequest request, Long variantId) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return;
        }

        Cart cart = getCart(request);
        cart.getCartItems().removeIf(item -> item.getVariant().getId().equals(variantId));
        cartRepository.save(cart);
    }

    // Tính tổng giá trị
    public double getTotalPrice(Cart cart) {
        return cart.getCartItems().stream()
                .mapToDouble(item -> item.getTotalPrice().doubleValue())
                .sum();
    }

    // Xóa toàn bộ giỏ hàng
    public void clearCart(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user != null) {
            Cart cart = getCart(request);
            if (cart != null) {
                cart.getCartItems().clear();
                cartRepository.save(cart);
            }
        }
    }
}