package com.safekid.mobile.network;

import com.safekid.mobile.network.dto.EmailRequest;
import com.safekid.mobile.network.dto.LoginRequest;
import com.safekid.mobile.network.dto.LoginResponse;
import com.safekid.mobile.network.dto.RegisterRequest;
import com.safekid.mobile.network.dto.ResetPasswordRequest;
import com.safekid.mobile.network.dto.VerifyEmailRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApi {

    @POST("auth/register")
    Call<Void> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/verify-email")
    Call<Void> verifyEmail(@Body VerifyEmailRequest request);

    @POST("auth/resend-verification")
    Call<Void> resendVerification(@Body EmailRequest request);

    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body EmailRequest request);

    @POST("auth/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    @POST("auth/child/qr-login")
    Call<LoginResponse> qrLogin(@Query("cocukUniqueId") String cocukUniqueId);
}
