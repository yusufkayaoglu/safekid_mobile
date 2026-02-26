package com.safekid.mobile.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.safekid.mobile.network.ApiClient;
import com.safekid.mobile.network.AuthApi;
import com.safekid.mobile.network.dto.EmailRequest;
import com.safekid.mobile.network.dto.LoginRequest;
import com.safekid.mobile.network.dto.LoginResponse;
import com.safekid.mobile.network.dto.RegisterRequest;
import com.safekid.mobile.network.dto.ResetPasswordRequest;
import com.safekid.mobile.network.dto.VerifyEmailRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private final AuthApi authApi;

    private final MutableLiveData<LoginResponse> loginResult    = new MutableLiveData<>();
    private final MutableLiveData<LoginResponse> qrLoginResult  = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> verifySuccess   = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final MutableLiveData<String>  error   = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authApi = ApiClient.getInstance().create(AuthApi.class);
    }

    // ── Exposed LiveData ──────────────────────────────────────────────────────

    public LiveData<LoginResponse> getLoginResult()   { return loginResult; }
    public LiveData<LoginResponse> getQrLoginResult() { return qrLoginResult; }
    public void clearLoginResult()   { loginResult.setValue(null); }
    public void clearQrLoginResult() { qrLoginResult.setValue(null); }
    public void clearOperationSuccess() { operationSuccess.setValue(null); }
    public LiveData<Boolean> getRegisterSuccess()  { return registerSuccess; }
    public LiveData<Boolean> getVerifySuccess()    { return verifySuccess; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }
    public LiveData<String>  getError()   { return error; }
    public LiveData<Boolean> isLoading()  { return loading; }

    // ── Login ─────────────────────────────────────────────────────────────────

    public void login(String email, String password) {
        loading.setValue(true);
        authApi.login(new LoginRequest(email, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponse> call,
                                           @NonNull Response<LoginResponse> response) {
                        loading.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            loginResult.postValue(response.body());
                        } else {
                            error.postValue("Giriş başarısız: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    // ── Register ──────────────────────────────────────────────────────────────

    public void register(RegisterRequest req) {
        loading.setValue(true);
        authApi.register(req).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                loading.postValue(false);
                if (response.isSuccessful()) {
                    registerSuccess.postValue(true);
                } else {
                    error.postValue("Kayıt başarısız: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                loading.postValue(false);
                error.postValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    // ── Verify Email ──────────────────────────────────────────────────────────

    public void verifyEmail(String email, String code) {
        loading.setValue(true);
        authApi.verifyEmail(new VerifyEmailRequest(email, code))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        loading.postValue(false);
                        if (response.isSuccessful()) {
                            verifySuccess.postValue(true);
                        } else {
                            error.postValue("Doğrulama başarısız: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    public void resendVerification(String email) {
        authApi.resendVerification(new EmailRequest(email))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            operationSuccess.postValue(true);
                        } else {
                            error.postValue("Gönderilemedi: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    // ── Forgot / Reset Password ───────────────────────────────────────────────

    public void forgotPassword(String email) {
        loading.setValue(true);
        authApi.forgotPassword(new EmailRequest(email))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        loading.postValue(false);
                        if (response.isSuccessful()) {
                            operationSuccess.postValue(true);
                        } else {
                            error.postValue("İstek başarısız: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    public void resetPassword(String email, String kod, String yeniSifre) {
        loading.setValue(true);
        authApi.resetPassword(new ResetPasswordRequest(email, kod, yeniSifre))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        loading.postValue(false);
                        if (response.isSuccessful()) {
                            operationSuccess.postValue(true);
                        } else {
                            error.postValue("Şifre güncellenemedi: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }

    // ── QR Login (Child) ──────────────────────────────────────────────────────

    public void qrLogin(String cocukUniqueId) {
        loading.setValue(true);
        authApi.qrLogin(cocukUniqueId)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponse> call,
                                           @NonNull Response<LoginResponse> response) {
                        loading.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            // Ayrı LiveData — LoginFragment'ı etkilemez
                            qrLoginResult.postValue(response.body());
                        } else {
                            error.postValue("QR giriş başarısız: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                        loading.postValue(false);
                        error.postValue("Bağlantı hatası: " + t.getMessage());
                    }
                });
    }
}
