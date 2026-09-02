package com.poly.asm.controller;

import com.poly.asm.daos.OrderRepository;
import com.poly.asm.daos.ProductVariantRepository;
import com.poly.asm.entitys.Order;
import com.poly.asm.entitys.OrderDetail;
import com.poly.asm.entitys.User;
import com.poly.asm.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String viewOrders(HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Order> orders = orderRepository.findByUser_Id(user.getId());
        model.addAttribute("orders", orders);
        return "web/orders";
    }

    @GetMapping("/{orderId}")
    public String viewOrderDetails(@PathVariable Long orderId, HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order != null && order.getUser().getId().equals(user.getId())) {
            List<OrderDetail> details = order.getOrderDetails();
            model.addAttribute("order", order);
            model.addAttribute("details", details);
            return "web/order-details";
        }

        model.addAttribute("error", "Đơn hàng không tồn tại hoặc không thuộc về bạn!");
        return "redirect:/orders";
    }

    /** Khách bấm "Đã nhận hàng" trong trang đơn hàng. */
    @PostMapping("/received/{orderId}")
    public String confirmReceived(@PathVariable Long orderId, HttpServletRequest request,
                                  RedirectAttributes redirect) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            redirect.addFlashAttribute("error", "Đơn hàng không tồn tại hoặc không thuộc về bạn!");
            return "redirect:/orders";
        }

        try {
            orderService.updateOrderStatus(orderId, "DELIVERED", "CUSTOMER");
            redirect.addFlashAttribute("message",
                    "Cảm ơn bạn đã xác nhận nhận hàng đơn #" + orderId + ".");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Không thể xác nhận: " + e.getMessage());
        }
        return "redirect:/orders";
    }

    /** Khách bấm link xác nhận đã nhận hàng trong email (không cần đăng nhập). */
    @GetMapping("/confirm-received/{orderId}")
    public String confirmReceivedByEmail(@PathVariable Long orderId, @RequestParam String token,
                                         Model model) {
        try {
            orderService.confirmReceivedByToken(orderId, token);
            model.addAttribute("orderId", orderId);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "web/order-received";
    }

    @PostMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findByIdWithDetails(orderId);
        if (order != null && order.getUser().getId().equals(user.getId())) {
            try {
                orderService.updateOrderStatus(orderId, "CANCELED");
                model.addAttribute("message", "Hủy đơn hàng thành công!");
            } catch (Exception e) {
                model.addAttribute("error", "Không thể hủy đơn hàng: " + e.getMessage());
            }
        } else {
            model.addAttribute("error", "Đơn hàng không tồn tại hoặc không thuộc về bạn!");
        }

        return "redirect:/orders";
    }
}