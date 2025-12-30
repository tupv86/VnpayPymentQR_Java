package com.tupv.PaymentQR.dto;
public class RefundInput {
    private String orderId;
    private long refundAmount; // VND

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(long refundAmount) { this.refundAmount = refundAmount; }
}
