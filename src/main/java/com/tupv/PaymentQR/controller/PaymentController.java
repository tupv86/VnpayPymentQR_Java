package com.tupv.PaymentQR.controller;
import com.tupv.PaymentQR.config.VnpayConfig;
import com.tupv.PaymentQR.model.Order;
import com.tupv.PaymentQR.service.OrderService;
import com.tupv.PaymentQR.vnpay.VnpayHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;
import java.util.TreeMap;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final VnpayConfig vnpayConfig;
    private final OrderService orderService;

    private static final DateTimeFormatter VNP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public PaymentController(VnpayConfig vnpayConfig, OrderService orderService) {
        this.vnpayConfig = vnpayConfig;
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public RedirectView createPayment(
            @RequestParam String amount,
            @RequestParam(name = "vnp_PromoCode", defaultValue = "") String vnpPromoCode,
            HttpServletRequest request
    ) {
        // validate amount
        long amountLong;
        try {
            amountLong = Long.parseLong(amount);
            if (amountLong <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be a positive integer");
        }

        String orderId = LocalDateTime.now().format(VNP_FMT);
        String expireDate = LocalDateTime.now().plusMinutes(15).format(VNP_FMT);

        // Save order
        Order order = new Order();
        order.setOrderId(orderId);
        order.setAmount(amountLong);
        order.setOrderInfo("Test thanh toan tien Thuoc");
        order.setCreateDate(LocalDateTime.now().format(VNP_FMT));
        order.setExpireDate(expireDate);
        order.setPromoCode(vnpPromoCode);
        order.setStatus("Pending");
        orderService.addOrder(order);

        String clientIp = getClientIp(request);

        // VNP params (sorted)
        SortedMap<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.3");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountLong * 100)); // nhân 100
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", "Test thanh toan tien Thuoc");
        vnpParams.put("vnp_OrderType", "oldmc");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", clientIp);
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(VNP_FMT));
        vnpParams.put("vnp_ExpireDate", expireDate);

        if (vnpPromoCode != null && !vnpPromoCode.isBlank()) {
            vnpParams.put("vnp_PromoCode", vnpPromoCode + amount);
        }

        VnpayHelper helper = new VnpayHelper(vnpayConfig.getHashSecret());
        String hashData = helper.buildQueryString(vnpParams);
        String secureHash = helper.hmacSHA512(hashData);

        String queryString = helper.buildQueryString(vnpParams);
        String paymentUrl = vnpayConfig.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

        return new RedirectView(paymentUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
