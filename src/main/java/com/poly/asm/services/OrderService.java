package com.poly.asm.services;

import com.poly.asm.ResourceNotFoundException;
import com.poly.asm.daos.OrderDetailRepository;
import com.poly.asm.daos.OrderRepository;
import com.poly.asm.daos.ProductVariantRepository;
import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.CartItem;
import com.poly.asm.entitys.Order;
import com.poly.asm.entitys.OrderDetail;
import com.poly.asm.entitys.ProductVariant;
import com.poly.asm.entitys.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    private static final List<String> VALID_STATUSES = Arrays.asList("PENDING", "SHIPPING", "DELIVERED", "CANCELED");

    private static final Map<String, String> STATUS_VI_MAPPING = new HashMap<>();
    static {
        STATUS_VI_MAPPING.put("PENDING", "Chờ xử lý");
        STATUS_VI_MAPPING.put("SHIPPING", "Đang giao");
        STATUS_VI_MAPPING.put("DELIVERED", "Đã giao");
        STATUS_VI_MAPPING.put("CANCELED", "Đã hủy");
    }

    // Phương thức để lấy trạng thái tiếng Việt
    public String getVietnameseStatus(String englishStatus) {
        return STATUS_VI_MAPPING.getOrDefault(englishStatus.toUpperCase(), "Không xác định");
    }
    
    /**
     * Tạo đơn hàng từ giỏ hàng và trừ tồn kho trong cùng một transaction.
     */
    @Transactional
    public Order placeOrder(Cart cart, User user, String fullname, String phone, String address,
                            String paymentMethod, double totalPrice) {
        Order order = new Order();
        order.setFullname(fullname);
        order.setPhone(phone);
        order.setAddress(address);
        order.setPaymentMethod(paymentMethod);
        order.setTotalPrice(totalPrice);
        order.setUser(user);
        order.setStatus("PENDING");
        order = orderRepository.save(order);

        for (CartItem cartItem : cart.getCartItems()) {
            ProductVariant variant = productVariantRepository.findById(cartItem.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Biến thể sản phẩm không tồn tại: " + cartItem.getVariant().getId()));
            if (variant.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("Sản phẩm '" + variant.getProduct().getName()
                        + "' chỉ còn " + variant.getStock() + " sản phẩm trong kho");
            }

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setVariant(variant);
            orderDetail.setQuantity(cartItem.getQuantity());
            orderDetail.setPrice(cartItem.getPrice());
            orderDetailRepository.save(orderDetail);

            variant.setStock(variant.getStock() - cartItem.getQuantity());
            productVariantRepository.save(variant);
        }
        return order;
    }

    @Transactional
    public void updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findByIdWithDetails(id);
        if (order == null) {
            throw new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + id);
        }

        if (!VALID_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + status);
        }

        // Kiểm tra trạng thái hợp lệ khi hủy
        if ("CANCELED".equalsIgnoreCase(status) && !"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng ở trạng thái Chờ xử lý");
        }

        // Nếu hủy đơn hàng, hoàn kho
        if ("CANCELED".equalsIgnoreCase(status)) {
            for (OrderDetail detail : order.getOrderDetails()) {
                ProductVariant variant = detail.getVariant();
                variant.setStock(variant.getStock() + detail.getQuantity());
                productVariantRepository.save(variant);
            }
        }

        order.setStatus(status.toUpperCase());
        orderRepository.save(order);
    }
}