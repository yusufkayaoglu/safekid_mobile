package com.safekid.mobile.network.dto;

public class AddChildRequest {
    public String cocukAdi;
    public String cocukSoyadi;

    public AddChildRequest(String adi, String soyadi) {
        this.cocukAdi = adi;
        this.cocukSoyadi = soyadi;
    }
}
