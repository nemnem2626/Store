package com.poly.asm.controller;

import com.poly.asm.entitys.Cart;
import com.poly.asm.entitys.CartItem;
import com.poly.asm.entitys.ProductVariant;
import com.poly.asm.entitys.User;
import com.poly.asm.services.CartService;
import com.poly.asm.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Controller
public class VNPAYController {

    private static final Logger logger = LoggerFactory.getLogger(VNPAYController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private VNPAYConfig vnpayConfig;

    @Autowired
    private OrderService orderService;

    @GetMapping("/vnpay-return")
    public String handleVNPayReturn(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.info("Nhận yêu cầu đến /vnpay-return");
        logger.info("Chuỗi truy vấn: {}", request.getQueryString());

        Map<String, String> vnpParams = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String value = entry.getValue()[0];
            try {
                value = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                logger.error("Lỗi giải mã tham số {}: {}", entry.getKey(), e.getMessage());
            }
            vnpParams.put(entry.getKey(), value);
        }

        logger.info("Tham số trả về từ VNPay: {}", vnpParams);
        String vnp_SecureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                }
            }
        }

        String serverHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());

        if (serverHash.equalsIgnoreCase(vnp_SecureHash)) {
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            logger.info("Mã phản hồi VNPay: {}", vnp_ResponseCode);
            if ("00".equals(vnp_ResponseCode)) {
                User user = (User) request.getSession().getAttribute("user");
                if (user == null) {
                    logger.error("Phiên đăng nhập không hợp lệ");
                    redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại!");
                    return "redirect:/login";
                }

                // Kiểm tra số điện thoại
                String phone = (String) request.getSession().getAttribute("order_phone");
                if (phone == null || !phone.matches("^\\d{10}$")) {
                    logger.error("Số điện thoại không hợp lệ: {}", phone);
                    redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ. Vui lòng cập nhật thông tin thanh toán!");
                    return "redirect:/cart/checkout";
                }

                // Kiểm tra địa chỉ
                String address = (String) request.getSession().getAttribute("order_address");
                if (address == null || address.trim().isEmpty()) {
                    logger.error("Địa chỉ không hợp lệ: {}", address);
                    redirectAttributes.addFlashAttribute("error", "Địa chỉ không hợp lệ. Vui lòng cập nhật thông tin thanh toán!");
                    return "redirect:/cart/checkout";
                }

                Cart cart = cartService.getCart(request);
                if (cart == null || cart.getCartItems().isEmpty()) {
                    logger.error("Giỏ hàng trống hoặc không hợp lệ");
                    redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống hoặc không hợp lệ!");
                    return "redirect:/cart/checkout";
                }

                for (CartItem cartItem : cart.getCartItems()) {
                    ProductVariant variant = cartItem.getVariant();
                    if (variant == null || variant.getStock() < cartItem.getQuantity()) {
                        logger.error("Sản phẩm không hợp lệ hoặc hết hàng: variantId={}, stock={}, quantity={}",
                                variant != null ? variant.getId() : "null",
                                variant != null ? variant.getStock() : 0,
                                cartItem.getQuantity());
                        redirectAttributes.addFlashAttribute("error", "Sản phẩm không hợp lệ hoặc đã hết hàng!");
                        return "redirect:/cart/checkout";
                    }
                }

                String fullname = (String) request.getSession().getAttribute("order_fullname");

                try {
                    orderService.placeOrder(cart, user, fullname, phone, address, "VNPAY",
                            cartService.getTotalPrice(cart));
                    cartService.clearCart(request);
                    // Xóa thông tin thanh toán khỏi session
                    request.getSession().removeAttribute("order_fullname");
                    request.getSession().removeAttribute("order_phone");
                    request.getSession().removeAttribute("order_address");
                    logger.info("Lưu đơn hàng thành công. Chuyển hướng đến /cart/success");
                    return "redirect:/cart/success";
                } catch (Exception e) {
                    logger.error("Lỗi khi lưu đơn hàng VNPay: {}", e.getMessage(), e);
                    redirectAttributes.addFlashAttribute("error",
                            "Thanh toán thành công nhưng không lưu được đơn hàng. Vui lòng liên hệ cửa hàng!");
                    return "redirect:/cart/checkout";
                }
            } else {
                String errorMessage = getVNPayErrorMessage(vnp_ResponseCode);
                logger.error("Lỗi thanh toán VNPay: {}", errorMessage);
                redirectAttributes.addFlashAttribute("error", errorMessage);
                return "redirect:/cart/checkout";
            }
        } else {
            logger.error("Chữ ký VNPay không hợp lệ cho giao dịch {}", vnpParams.get("vnp_TxnRef"));
            redirectAttributes.addFlashAttribute("error", "Sai chữ ký! Vui lòng thử lại.");
            return "redirect:/cart/checkout";
        }
    }

    private String getVNPayErrorMessage(String responseCode) {
        switch (responseCode) {
            case "07": return "Giao dịch bị nghi ngờ gian lận.";
            case "09": return "Thẻ/Tài khoản chưa đăng ký dịch vụ Internet Banking.";
            case "10": return "Xác thực không thành công. Vui lòng kiểm tra thông tin thẻ.";
            case "11": return "Giao dịch chưa được xử lý. Vui lòng thử lại sau.";
            case "12": return "Thẻ/Tài khoản của bạn đã bị khóa.";
            case "13": return "Xác thực OTP không thành công.";
            case "24": return "Giao dịch bị hủy bởi người dùng.";
            case "51": return "Số tiền không đủ để thực hiện giao dịch.";
            default: return "Lỗi thanh toán VNPay: Mã lỗi " + responseCode;
        }
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

}
