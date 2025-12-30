package com.tupv.PaymentQR.model;

public class Order {
    private String orderId;
    private long amount;
    private String orderInfo;
    private String createDate;
    private String expireDate;
    private String promoCode;
    private String status;
    private String transactionNo;

    public Order() {}

    public Order(String orderId, long amount, String orderInfo, String createDate, String status) {
        this.orderId = orderId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.createDate = createDate;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getOrderInfo() { return orderInfo; }
    public void setOrderInfo(String orderInfo) { this.orderInfo = orderInfo; }

    public String getCreateDate() { return createDate; }
    public void setCreateDate(String createDate) { this.createDate = createDate; }

    public String getExpireDate() { return expireDate; }
    public void setExpireDate(String expireDate) { this.expireDate = expireDate; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
}
