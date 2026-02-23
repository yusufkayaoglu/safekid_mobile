package com.safekid.mobile.network.dto;

public class AddChildRequest {
    public String cocukAdi;
    public String cocukSoyadi;
    public String cocukTelefonNo;
    public String cocukMail;

    public AddChildRequest(String adi, String soyadi, String telefon, String mail) {
        this.cocukAdi = adi;
        this.cocukSoyadi = soyadi;
        this.cocukTelefonNo = telefon;
        this.cocukMail = mail;
    }
}
