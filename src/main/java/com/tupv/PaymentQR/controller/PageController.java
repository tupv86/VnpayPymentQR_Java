package com.tupv.PaymentQR.controller;
import com.tupv.PaymentQR.model.Order;
import com.tupv.PaymentQR.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PageController {

    private final OrderService orderService;

    public PageController(OrderService orderService) {
        this.orderService = orderService;
    }
    @GetMapping("/")
    public String index(Model model) {
        // demo data (bạn thay bằng OrderService/DB sau)
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("title", "Danh sách giao dịch");
        model.addAttribute("content", "index");
        model.addAttribute("activePage", "index");
        model.addAttribute("orders", orders);
        return "layout";
    }

    @GetMapping("/payment-form")
    public String paymentForm(Model model) {
        model.addAttribute("title", "Khởi tạo giao dịch");
        model.addAttribute("content", "payment-form");
        model.addAttribute("activePage", "paymentForm");
        return "layout";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy");
        model.addAttribute("activePage", "");
        model.addAttribute("content", "index :: fragment");
        model.addAttribute("pageScripts", "index :: scripts");
        return "layout";
    }
}
