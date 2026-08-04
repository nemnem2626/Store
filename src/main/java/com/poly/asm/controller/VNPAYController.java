package com.poly.asm.controller;

import com.poly.asm.daos.OrderDetailRepository;
import com.poly.asm.daos.OrderRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Controller
public class VNPAYController {

    private static final Logger logger = LoggerFactory.getLogger(VNPAYController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @GetMapping("/vnpay-return")
    public String handleVNPayReturn(HttpServletRequest request, Model model, HttpServletResponse response) {
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

        logger.info("HashData trả về: {}", hashData.toString());
        String serverHash = hmacSHA512(VNPAYConfig.VNP_HASH_SECRET, hashData.toString());
        logger.info("ServerHash: {}, VNP_SecureHash: {}", serverHash, vnp_SecureHash);

        if (serverHash.equalsIgnoreCase(vnp_SecureHash)) {
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            logger.info("Mã phản hồi VNPay: {}", vnp_ResponseCode);
            if ("00".equals(vnp_ResponseCode)) {
                User user = (User) request.getSession().getAttribute("user");
                if (user == null) {
                    logger.error("Phiên đăng nhập không hợp lệ");
                    model.addAttribute("error", "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại!");
                    return "redirect:/login";
                }

                // Kiểm tra số điện thoại
                String phone = (String) request.getSession().getAttribute("order_phone");
                if (phone == null || !phone.matches("^\\d{10}$")) {
                    logger.error("Số điện thoại không hợp lệ: {}", phone);
                    model.addAttribute("error", "Số điện thoại không hợp lệ. Vui lòng cập nhật thông tin thanh toán!");
                    return "redirect:/cart/checkout";
                }

                // Kiểm tra địa chỉ
                String address = (String) request.getSession().getAttribute("order_address");
                if (address == null || address.trim().isEmpty()) {
                    logger.error("Địa chỉ không hợp lệ: {}", address);
                    model.addAttribute("error", "Địa chỉ không hợp lệ. Vui lòng cập nhật thông tin thanh toán!");
                    return "redirect:/cart/checkout";
                }

                Cart cart = cartService.getCart(request);
                if (cart == null || cart.getCartItems().isEmpty()) {
                    logger.error("Giỏ hàng trống hoặc không hợp lệ");
                    model.addAttribute("error", "Giỏ hàng trống hoặc không hợp lệ!");
                    return "redirect:/cart/checkout";
                }

                for (CartItem cartItem : cart.getCartItems()) {
                    ProductVariant variant = cartItem.getVariant();
                    if (variant == null || variant.getStock() < cartItem.getQuantity()) {
                        logger.error("Sản phẩm không hợp lệ hoặc hết hàng: variantId={}, stock={}, quantity={}",
                                variant != null ? variant.getId() : "null",
                                variant != null ? variant.getStock() : 0,
                                cartItem.getQuantity());
                        model.addAttribute("error", "Sản phẩm không hợp lệ hoặc đã hết hàng!");
                        return "redirect:/cart/checkout";
                    }
                }

                Order order = new Order();
                order.setFullname((String) request.getSession().getAttribute("order_fullname"));
                order.setPhone(phone);
                order.setAddress(address);
                order.setPaymentMethod("VNPAY");
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
                    // Xóa thông tin thanh toán khỏi session
                    request.getSession().removeAttribute("order_fullname");
                    request.getSession().removeAttribute("order_phone");
                    request.getSession().removeAttribute("order_address");
                    logger.info("Lưu đơn hàng thành công. Chuyển hướng đến /cart/success");
                    return "redirect:/cart/success";
                } catch (Exception e) {
                    logger.error("Lỗi khi lưu đơn hàng: {}", e.getMessage(), e);
                    model.addAttribute("error", "Lỗi khi lưu đơn hàng: " + e.getMessage());
                    return "redirect:/cart/checkout";
                }
            } else {
                String errorMessage = getVNPayErrorMessage(vnp_ResponseCode);
                logger.error("Lỗi thanh toán VNPay: {}", errorMessage);
                model.addAttribute("error", errorMessage);
                return "redirect:/cart/checkout";
            }
        } else {
            logger.error("Sai chữ ký! ServerHash={}, VNP_SecureHash={}", serverHash, vnp_SecureHash);
            model.addAttribute("error", "Sai chữ ký! Vui lòng thử lại.");
            return "redirect:/cart/checkout";
        }
    }

    @GetMapping("/test-vnpay")
    public String testVNPay(HttpServletRequest request) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPAYConfig.VNP_VERSION);
        vnp_Params.put("vnp_Command", VNPAYConfig.VNP_COMMAND);
        vnp_Params.put("vnp_TmnCode", VNPAYConfig.VNP_TMN_CODE);
        vnp_Params.put("vnp_Amount", "1000000"); // 10,000 VND
        vnp_Params.put("vnp_CurrCode", VNPAYConfig.VNP_CURR_CODE);
        vnp_Params.put("vnp_TxnRef", "TEST" + System.currentTimeMillis());
        vnp_Params.put("vnp_OrderInfo", "Test payment");
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

        String vnp_SecureHash = hmacSHA512(VNPAYConfig.VNP_HASH_SECRET, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        String paymentUrl = VNPAYConfig.VNP_PAY_URL + "?" + query;
        logger.info("URL thanh toán thử: {}", paymentUrl);
        return "redirect:" + paymentUrl;
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

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}