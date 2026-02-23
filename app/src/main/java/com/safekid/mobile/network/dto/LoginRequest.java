package com.safekid.mobile.network.dto;

public class LoginRequest {
    public String ebeveynMailAdres;
    public String ebeveynPassword;

    public LoginRequest(String email, String password) {
        this.ebeveynMailAdres = email;
        this.ebeveynPassword = password;
    }
}
