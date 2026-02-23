package com.safekid.mobile.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.safekid.mobile.network.ApiClient;

public class SessionManager {

    private static final String PREF_NAME = "safekid_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_PARENT_ID = "parent_id";
    private static final String KEY_PARENT_NAME = "parent_name";
    private static final String KEY_PARENT_SURNAME = "parent_surname";
    private static final String KEY_ROLE = "role";

    public static final String ROLE_PARENT = "PARENT";
    public static final String ROLE_CHILD = "CHILD";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public void saveLogin(String token, String expiresAt,
                          String parentId, String name, String surname) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_EXPIRES_AT, expiresAt)
                .putString(KEY_PARENT_ID, parentId)
                .putString(KEY_PARENT_NAME, name)
                .putString(KEY_PARENT_SURNAME, surname)
                .putString(KEY_ROLE, ROLE_PARENT)
                .apply();
        ApiClient.invalidateAuthCache();
    }

    public void saveChildLogin(String token, String expiresAt,
                               String parentId, String parentName, String parentSurname) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_EXPIRES_AT, expiresAt)
                .putString(KEY_PARENT_ID, parentId)
                .putString(KEY_PARENT_NAME, parentName)
                .putString(KEY_PARENT_SURNAME, parentSurname)
                .putString(KEY_ROLE, ROLE_CHILD)
                .apply();
        ApiClient.invalidateAuthCache();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getExpiresAt() {
        return prefs.getString(KEY_EXPIRES_AT, null);
    }

    public String getParentId() {
        return prefs.getString(KEY_PARENT_ID, null);
    }

    public String getParentName() {
        return prefs.getString(KEY_PARENT_NAME, null);
    }

    public String getParentSurname() {
        return prefs.getString(KEY_PARENT_SURNAME, null);
    }

    public String getFullName() {
        String name = getParentName();
        String surname = getParentSurname();
        if (name == null) return "";
        return surname != null ? name + " " + surname : name;
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    // ── State checks ──────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public boolean isParent() {
        return ROLE_PARENT.equals(getRole());
    }

    public boolean isChild() {
        return ROLE_CHILD.equals(getRole());
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    public void clear() {
        prefs.edit().clear().apply();
        ApiClient.invalidateAuthCache();
    }
}
