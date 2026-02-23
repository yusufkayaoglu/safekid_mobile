package com.safekid.mobile.network.dto;

public class VerifyEmailRequest {
    public String ebeveynMailAdres;
    public String dogrulamaKodu;

    public VerifyEmailRequest(String email, String code) {
        this.ebeveynMailAdres = email;
        this.dogrulamaKodu = code;
    }
}
