package com.tupv.PaymentQR.controller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tupv.PaymentQR.dto.QueryDrInput;
import com.tupv.PaymentQR.dto.RefundInput;
import com.tupv.PaymentQR.model.Order;
import com.tupv.PaymentQR.service.OrderService;
import com.tupv.PaymentQR.config.VnpayConfig;
import com.tupv.PaymentQR.model.Order;
import com.tupv.PaymentQR.vnpay.VnpayHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/admin")
public class PaymentAdminController {

    private final VnpayConfig vnpayConfig;
    private final OrderService orderService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final DateTimeFormatter VNP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final String TMN_CODE = "YOUR_TMN_CODE";
    private final String HASH_SECRET = "YOUR_HASH_SECRET";

    public PaymentAdminController(VnpayConfig vnpayConfig,OrderService orderService) {
        this.vnpayConfig = vnpayConfig;
        this.orderService = orderService;
    }

    // ===================== QUERY DR =====================
    @PostMapping("/querydr")
    public ResponseEntity<?> queryDR(
            @RequestBody QueryDrInput input,
            HttpServletRequest request
    ) {
        if (input == null || input.getOrderId() == null || input.getOrderId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "orderId không hợp lệ"));
        }
        Optional<Order> opt = orderService.getOrderById(input.getOrderId());
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Không tìm thấy đơn hàng"));
        }
        Order order = opt.get();
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String createDate = LocalDateTime.now().format(VNP_FMT);
        String ipAddr = request.getRemoteAddr();

        // Chuỗi ký QUERYDR (GIỮ NGUYÊN THỨ TỰ)
        String data = String.join("|",
                requestId,
                "2.1.3",
                "querydr",
                vnpayConfig.getTmnCode(),
                order.getOrderId(),
                "",
                order.getCreateDate(),
                createDate,
                ipAddr,
                order.getOrderInfo()
        );


        VnpayHelper helper = new VnpayHelper(vnpayConfig.getHashSecret());
        String secureHash = helper.hmacSHA512(data);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("vnp_RequestId", requestId);
        payload.put("vnp_Version", "2.1.3");
        payload.put("vnp_Command", "querydr");
        payload.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        payload.put("vnp_TxnRef", order.getOrderId());
        payload.put("vnp_TransactionDate", order.getCreateDate());
        payload.put("vnp_CreateDate", createDate);
        payload.put("vnp_IpAddr", ipAddr);
        payload.put("vnp_OrderInfo", order.getOrderInfo());
        payload.put("vnp_SecureHash", secureHash);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "VNPAY-Java-Client");
            HttpEntity<String> entity =
                    new HttpEntity<>(mapper.writeValueAsString(payload), headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(vnpayConfig.getQueryUrl(), entity, String.class);

            JsonNode json = mapper.readTree(response.getBody());

            // ===== verify response hash =====
            String respHash = json.path("vnp_SecureHash").asText();
            if (respHash.isBlank()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Không có chữ ký từ VNPAY"));
            }
            String verifyData = buildVerifyDataQueryDr(json);
            String calcHash = helper.hmacSHA512(verifyData);
            if (!calcHash.equalsIgnoreCase(respHash)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Chữ ký response không hợp lệ"));
            }

            // ===== update order =====
            String respCode = json.path("vnp_ResponseCode").asText();
            String txnStatus = json.path("vnp_TransactionStatus").asText();
            String transactionNo = json.path("vnp_TransactionNo").asText();
            if ("00".equals(respCode) && "00".equals(txnStatus)) {
                order.setStatus("Success");
                order.setTransactionNo(transactionNo);
            } else {
                order.setStatus("Failed");
            }
            orderService.updateOrder(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "QueryDR success",
                    "status", order.getStatus(),
                    "vnpayRaw", response
            ));


        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Lỗi hệ thống: " + ex.getMessage()));
        }
    }

    // ===================== REFUND =====================
    @PostMapping("/refund")
    public ResponseEntity<?> refund(
            @RequestBody RefundInput input,
            HttpServletRequest request
    ) {
        if (input == null || input.getRefundAmount() <= 0 || input.getOrderId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Dữ liệu không hợp lệ"));
        }

        Optional<Order> opt = orderService.getOrderById(input.getOrderId());
        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Không tìm thấy đơn hàng"));
        }

        Order order = opt.get();
        if (!"Success".equals(order.getStatus())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Chỉ hoàn tiền cho giao dịch thành công"));
        }

        if (input.getRefundAmount() > order.getAmount()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Số tiền hoàn vượt quá số tiền giao dịch"));
        }

        String transType = input.getRefundAmount() == order.getAmount() ? "02" : "03";
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String createDate = LocalDateTime.now().format(VNP_FMT);
        String ipAddr = request.getRemoteAddr();

        String data = String.join("|",
                requestId,
                "2.1.0",
                "refund",
                vnpayConfig.getTmnCode(),
                transType,
                order.getOrderId(),
                String.valueOf(input.getRefundAmount() * 100),
                order.getTransactionNo(),
                order.getCreateDate(),
                "admin",
                createDate,
                ipAddr,
                "Hoàn tiền đơn hàng " + order.getOrderId()
        );
        VnpayHelper helper = new VnpayHelper(vnpayConfig.getHashSecret());
        String secureHash = helper.hmacSHA512(data);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("vnp_RequestId", requestId);
        payload.put("vnp_Version", "2.1.0");
        payload.put("vnp_Command", "refund");
        payload.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        payload.put("vnp_TransactionType", transType);
        payload.put("vnp_TxnRef", order.getOrderId());
        payload.put("vnp_Amount", input.getRefundAmount() * 100);
        payload.put("vnp_TransactionNo", order.getTransactionNo());
        payload.put("vnp_TransactionDate", order.getCreateDate());
        payload.put("vnp_CreateBy", "admin");
        payload.put("vnp_CreateDate", createDate);
        payload.put("vnp_IpAddr", ipAddr);
        payload.put("vnp_OrderInfo", "Hoàn tiền đơn hàng " + order.getOrderId());
        payload.put("vnp_SecureHash", secureHash);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "VNPAY-Java-Client");
            HttpEntity<String> entity =
                    new HttpEntity<>(mapper.writeValueAsString(payload), headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(vnpayConfig.getQueryUrl(), entity, String.class);

            JsonNode json = mapper.readTree(response.getBody());
            String respCode = json.path("vnp_ResponseCode").asText();
            String txnStatus = json.path("vnp_TransactionStatus").asText();

            // Các trạng thái hoàn tiền được coi là thành công
            boolean refundSuccess =
                    "00".equals(respCode) &&
                            ("00".equals(txnStatus) || "05".equals(txnStatus) || "06".equals(txnStatus));

            if (refundSuccess) {
                order.setStatus("Refunded");
                orderService.updateOrder(order);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Hoàn tiền thành công"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Hoàn tiền chưa thành công",
                        "responseCode", respCode,
                        "transactionStatus", txnStatus
                ));
            }

        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    private String buildVerifyDataQueryDr(JsonNode json) {
        String version = json.path("vnp_Version").asText("2.1.3").trim();

        // helper: lấy text, nếu null/missing -> ""
        java.util.function.Function<String, String> v =
                (k) -> json.path(k).asText("");

        if ("2.1.3".equals(version)) {
            // Theo spec bạn đưa: có thêm vnp_Trace, vnp_FeeAmount, vnp_CurrCode, vnp_CardNumber,...
            return String.join("|",
                    v.apply("vnp_ResponseId"),
                    v.apply("vnp_Command"),
                    v.apply("vnp_ResponseCode"),
                    v.apply("vnp_Message"),
                    v.apply("vnp_TmnCode"),
                    v.apply("vnp_TxnRef"),
                    v.apply("vnp_Trace"),
                    v.apply("vnp_Amount"),
                    v.apply("vnp_FeeAmount"),
                    v.apply("vnp_CurrCode"),
                    v.apply("vnp_BankCode"),
                    v.apply("vnp_CardNumber"),
                    v.apply("vnp_CardHolder"),
                    v.apply("vnp_MobileNumber"),
                    v.apply("vnp_PayDate"),
                    v.apply("vnp_TransactionNo"),
                    v.apply("vnp_TransactionType"),
                    v.apply("vnp_TransactionStatus"),
                    v.apply("vnp_OrderInfo"),
                    v.apply("vnp_PromotionCode"),
                    v.apply("vnp_PromotionAmount"),
                    v.apply("vnp_CardType"),
                    v.apply("vnp_PayType"),
                    v.apply("vnp_AccountType"),
                    v.apply("vnp_Issuer"),
                    v.apply("vnp_ApprovalCode")
            );
        }

        // Default: 2.1.0 (như bạn đang làm)
        return String.join("|",
                v.apply("vnp_ResponseId"),
                v.apply("vnp_Command"),
                v.apply("vnp_ResponseCode"),
                v.apply("vnp_Message"),
                v.apply("vnp_TmnCode"),
                v.apply("vnp_TxnRef"),
                v.apply("vnp_Amount"),
                v.apply("vnp_BankCode"),
                v.apply("vnp_PayDate"),
                v.apply("vnp_TransactionNo"),
                v.apply("vnp_TransactionType"),
                v.apply("vnp_TransactionStatus"),
                v.apply("vnp_OrderInfo"),
                v.apply("vnp_PromotionCode"),
                v.apply("vnp_PromotionAmount")
        );
    }

}
