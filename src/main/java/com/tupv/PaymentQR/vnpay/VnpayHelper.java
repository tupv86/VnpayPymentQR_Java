package com.tupv.PaymentQR.vnpay;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

public class VnpayHelper {

    private final String hashSecret;

    public VnpayHelper(String hashSecret) {
        this.hashSecret = hashSecret;
    }

    // VNPAY thường dùng HMACSHA512 cho vnp_SecureHash
    public String hmacSHA512(String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create HMAC SHA512", e);
        }
    }

    // Data ký: key=value&key=value... (đã sort theo key)
    public String buildHashData(Map<String, String> params) {
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                sj.add(e.getKey() + "=" + e.getValue());
            }
        }
        return sj.toString();
    }

    // Query string redirect (urlencode value)
    public String buildQueryString(Map<String, String> params) {
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                String encoded = URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8);
                sj.add(e.getKey() + "=" + encoded);
            }
        }
        return sj.toString();
    }
}
