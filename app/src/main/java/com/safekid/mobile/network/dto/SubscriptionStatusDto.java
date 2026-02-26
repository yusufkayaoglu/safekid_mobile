package com.safekid.mobile.network.dto;

public class SubscriptionStatusDto {
    public boolean premium;
    public String subscriptionType;  // "FREE" | "MONTHLY" | "YEARLY"
    public String expiresAt;         // ISO-8601 veya null
    public int maxChildren;          // 1 (free) veya -1 (sınırsız)
    public boolean geofenceEnabled;
    public boolean aiEnabled;
}
