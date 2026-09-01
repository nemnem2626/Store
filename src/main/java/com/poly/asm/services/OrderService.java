package com.poly.asm.services;

import com.poly.asm.ResourceNotFoundException;
import com.poly.asm.daos.NotificationRepository;
import com.poly.asm.daos.OrderDetailRepository;
import com.poly.asm.daos.OrderRepository;
import com.poly.asm.daos.ProductVariantRepository;
import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.CartItem;
import com.poly.asm.entitys.Notification;
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

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MailService mailService;

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

    /** Đơn đã giao hoặc đã hủy là trạng thái cuối, không cho cập nhật nữa. */
    public boolean isLocked(Order order) {
        String status = order.getStatus() == null ? "PENDING" : order.getStatus().toUpperCase();
        return "DELIVERED".equals(status) || "CANCELED".equals(status);
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
        updateOrderStatus(id, status, "STAFF");
    }

    /**
     * Cập nhật trạng thái đơn hàng theo vai trò người thực hiện.
     * Chỉ staff được xác nhận giao hàng (PENDING -> SHIPPING), việc đó sinh thông báo cho admin.
     * Khi chuyển sang DELIVERED, khách nhận email xác nhận.
     */
    @Transactional
    public void updateOrderStatus(Long id, String status, String actorRole) {
        Order order = orderRepository.findByIdWithDetails(id);
        if (order == null) {
            throw new ResourceNotFoundException("Đơn hàng không tồn tại với ID: " + id);
        }

        String newStatus = status.toUpperCase();
        String currentStatus = order.getStatus() == null ? "PENDING" : order.getStatus().toUpperCase();

        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + status);
        }

        if (isLocked(order)) {
            throw new IllegalStateException("Đơn hàng đã " + getVietnameseStatus(currentStatus).toLowerCase()
                    + ", không thể cập nhật trạng thái nữa");
        }

        if (newStatus.equals(currentStatus)) {
            return;
        }

        if ("PENDING".equals(newStatus)) {
            throw new IllegalStateException("Không thể đưa đơn hàng trở lại trạng thái Chờ xử lý");
        }

        // Xác nhận giao hàng là quyền của staff, admin chỉ theo dõi
        if ("SHIPPING".equals(newStatus) && !"STAFF".equalsIgnoreCase(actorRole)) {
            throw new IllegalStateException("Chuyển sang Đang giao phải do staff xác nhận");
        }

        if ("DELIVERED".equals(newStatus) && !"SHIPPING".equals(currentStatus)) {
            throw new IllegalStateException("Chỉ có thể xác nhận Đã giao khi đơn đang ở trạng thái Đang giao");
        }

        // Kiểm tra trạng thái hợp lệ khi hủy
        if ("CANCELED".equals(newStatus) && !"PENDING".equals(currentStatus)) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng ở trạng thái Chờ xử lý");
        }

        // Nếu hủy đơn hàng, hoàn kho
        if ("CANCELED".equals(newStatus)) {
            for (OrderDetail detail : order.getOrderDetails()) {
                ProductVariant variant = detail.getVariant();
                variant.setStock(variant.getStock() + detail.getQuantity());
                productVariantRepository.save(variant);
            }
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        if ("SHIPPING".equals(newStatus)) {
            Notification notification = new Notification();
            notification.setTargetRole("ADMIN");
            notification.setOrderId(order.getId());
            notification.setContent("Staff đã xác nhận giao đơn hàng #" + order.getId()
                    + " của khách " + order.getFullname());
            notificationRepository.save(notification);
        }

        if ("DELIVERED".equals(newStatus)) {
            mailService.sendOrderDelivered(order);
        }
    }
}