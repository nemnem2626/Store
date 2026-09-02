package com.poly.asm.services;

import java.text.NumberFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import com.poly.asm.entitys.Order;
import com.poly.asm.entitys.OrderDetail;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);
    private static final Locale MONEY_LOCALE = Locale.forLanguageTag("vi-VN");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:no-reply@store.vn}")
    private String from;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /** Gửi email báo đơn đang giao kèm nút để khách bấm xác nhận đã nhận hàng. */
    public void sendOrderShipped(Order order) {
        String url = confirmUrl(order);
        String html = "<div style=\"font-family:Arial,sans-serif;font-size:15px;color:#212529\">"
                + "<p>Xin chào <strong>" + escape(order.getFullname()) + "</strong>,</p>"
                + "<p>Đơn hàng <strong>#" + order.getId() + "</strong> của bạn đang được giao.</p>"
                + "<p>Tổng tiền: <strong>" + money().format(order.getTotalPrice()) + " VNĐ</strong><br>"
                + "Địa chỉ nhận: " + escape(order.getAddress()) + "</p>"
                + "<p>Khi nhận được hàng, bạn vui lòng bấm nút bên dưới để xác nhận:</p>"
                + "<p><a href=\"" + url + "\" style=\"display:inline-block;background:#198754;color:#fff;"
                + "text-decoration:none;padding:12px 26px;border-radius:30px;font-weight:bold\">"
                + "Đã nhận hàng</a></p>"
                + "<p style=\"font-size:13px;color:#6c757d\">Nếu nút không bấm được, hãy mở link sau: <br>"
                + "<a href=\"" + url + "\">" + url + "</a></p>"
                + "<p style=\"font-size:13px;color:#6c757d\">Nếu chưa nhận được hàng, bạn cứ bỏ qua email này.</p>"
                + "</div>";
        sendHtml(order, "STORE - Đơn hàng #" + order.getId() + " đang được giao", html);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String confirmUrl(Order order) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return root + "/orders/confirm-received/" + order.getId() + "?token=" + order.getConfirmToken();
    }

    /** Gửi email xác nhận đã giao hàng cho khách; bỏ qua khi chưa cấu hình SMTP. */
    public void sendOrderDelivered(Order order) {
        send(order, "STORE - Đơn hàng #" + order.getId() + " đã được giao thành công", buildBody(order));
    }

    /** Gửi email dạng HTML (có nút bấm) cho khách. */
    private void sendHtml(Order order, String subject, String html) {
        String to = recipient(order);
        if (to == null) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            logger.info("Đã gửi email đơn {} tới {}", order.getId(), to);
        } catch (Exception e) {
            logger.error("Gửi email đơn {} thất bại: {}", order.getId(), e.getMessage());
        }
    }

    /** Trả về email khách, hoặc null khi chưa bật SMTP / khách không có email. */
    private String recipient(Order order) {
        String to = order.getUser() != null ? order.getUser().getEmail() : null;
        if (!enabled || mailSender == null) {
            logger.info("Bỏ qua email đơn {} vì chưa bật cấu hình SMTP", order.getId());
            return null;
        }
        if (to == null || to.isBlank()) {
            logger.warn("Đơn {} không có email khách hàng, không gửi được email", order.getId());
            return null;
        }
        return to;
    }

    private void send(Order order, String subject, String text) {
        String to = recipient(order);
        if (to == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
            logger.info("Đã gửi email đơn {} tới {}", order.getId(), to);
        } catch (Exception e) {
            logger.error("Gửi email đơn {} thất bại: {}", order.getId(), e.getMessage());
        }
    }

    private NumberFormat money() {
        return NumberFormat.getInstance(MONEY_LOCALE);
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
                        .append(": ").append(money().format(detail.getPrice())).append(" VNĐ\n");
            }
        }
        body.append("\nTổng tiền: ").append(money().format(order.getTotalPrice())).append(" VNĐ\n");
        body.append("Địa chỉ nhận: ").append(order.getAddress()).append("\n\n");
        body.append("Cảm ơn bạn đã mua sắm tại STORE. Mọi thắc mắc xin liên hệ hotline 012 345 6789.\n");
        return body.toString();
    }
}
