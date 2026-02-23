package com.safekid.mobile.network.dto;

public class ResetPasswordRequest {
    public String ebeveynMailAdres;
    public String kod;
    public String yeniSifre;

    public ResetPasswordRequest(String email, String kod, String yeniSifre) {
        this.ebeveynMailAdres = email;
        this.kod = kod;
        this.yeniSifre = yeniSifre;
    }
}
