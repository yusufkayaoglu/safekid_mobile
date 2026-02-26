package com.safekid.mobile.network.dto;

public class ChildDto {
    public String cocukUniqueId;
    public String cocukAdi;
    public String cocukSoyadi;

    public String getFullName() {
        return cocukAdi + " " + (cocukSoyadi != null ? cocukSoyadi : "");
    }
}
