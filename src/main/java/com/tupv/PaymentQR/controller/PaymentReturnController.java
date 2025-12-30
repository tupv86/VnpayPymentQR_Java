package com.tupv.PaymentQR.controller;
import com.tupv.PaymentQR.model.VnpayResponseModel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentReturnController {

    @GetMapping("/payment/return")
    public String paymentReturn(HttpServletRequest request, Model model) {

        VnpayResponseModel res = new VnpayResponseModel();
        res.setVnp_Amount(request.getParameter("vnp_Amount"));
        res.setVnp_BankCode(request.getParameter("vnp_BankCode"));
        res.setVnp_BankTranNo(request.getParameter("vnp_BankTranNo"));
        res.setVnp_CardType(request.getParameter("vnp_CardType"));
        res.setVnp_OrderInfo(request.getParameter("vnp_OrderInfo"));
        res.setVnp_PayDate(request.getParameter("vnp_PayDate"));
        res.setVnp_ResponseCode(request.getParameter("vnp_ResponseCode"));
        res.setVnp_TmnCode(request.getParameter("vnp_TmnCode"));
        res.setVnp_TransactionNo(request.getParameter("vnp_TransactionNo"));
        res.setVnp_TransactionStatus(request.getParameter("vnp_TransactionStatus"));
        res.setVnp_TxnRef(request.getParameter("vnp_TxnRef"));
        res.setVnp_SecureHash(request.getParameter("vnp_SecureHash"));

        // giống View("PaymentResult", model)
        model.addAttribute("model", res);
        model.addAttribute("title", "Kết quả thanh toán");
        model.addAttribute("activePage", "");
        model.addAttribute("content", "payment-result");
        return "layout";
    }
}
