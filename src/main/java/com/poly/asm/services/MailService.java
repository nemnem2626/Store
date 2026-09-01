package com.poly.asm.services;

import java.text.NumberFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.poly.asm.entitys.Order;
import com.poly.asm.entitys.OrderDetail;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final NumberFormat MONEY = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@store.vn}")
    private String from;

    /** Gửi email xác nhận đã giao hàng cho khách; bỏ qua khi chưa cấu hình SMTP. */
    public void sendOrderDelivered(Order order) {
        String to = order.getUser() != null ? order.getUser().getEmail() : null;
        if (!enabled || mailSender == null) {
            logger.info("Bỏ qua email giao hàng cho đơn {} vì chưa bật cấu hình SMTP", order.getId());
            return;
        }
        if (to == null || to.isBlank()) {
            logger.warn("Đơn {} không có email khách hàng, không gửi được xác nhận", order.getId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("STORE - Đơn hàng #" + order.getId() + " đã được giao thành công");
        message.setText(buildBody(order));
        try {
            mailSender.send(message);
            logger.info("Đã gửi email xác nhận giao hàng đơn {} tới {}", order.getId(), to);
        } catch (Exception e) {
            logger.error("Gửi email xác nhận giao hàng đơn {} thất bại: {}", order.getId(), e.getMessage());
        }
    }

    private String buildBody(Order order) {
        StringBuilder body = new StringBuilder();
        body.append("Xin chào ").append(order.getFullname()).append(",\n\n");
        body.append("Đơn hàng #").append(order.getId()).append(" của bạn đã được giao thành công.\n\n");
        if (order.getOrderDetails() != null) {
            body.append("Sản phẩm:\n");
            for (OrderDetail detail : order.getOrderDetails()) {
                body.append(" - ").append(detail.getVariant().getProduct().getName())
                        .append(" x").append(detail.getQuantity())
                        .append(": ").append(MONEY.format(detail.getPrice())).append(" VNĐ\n");
            }
        }
        body.append("\nTổng tiền: ").append(MONEY.format(order.getTotalPrice())).append(" VNĐ\n");
        body.append("Địa chỉ nhận: ").append(order.getAddress()).append("\n\n");
        body.append("Cảm ơn bạn đã mua sắm tại STORE. Mọi thắc mắc xin liên hệ hotline 012 345 6789.\n");
        return body.toString();
    }
}
