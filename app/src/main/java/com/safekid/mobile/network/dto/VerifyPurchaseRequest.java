package com.safekid.mobile.network.dto;

public class VerifyPurchaseRequest {
    public String purchaseToken;
    public String productId;
    public String packageName;

    public VerifyPurchaseRequest(String purchaseToken, String productId, String packageName) {
        this.purchaseToken = purchaseToken;
        this.productId = productId;
        this.packageName = packageName;
    }
}
