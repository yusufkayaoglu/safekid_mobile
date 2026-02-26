package com.safekid.mobile.network;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // ── 401 Unauthorized callback ─────────────────────────────────────────────
    /** MainActivity tarafından set edilir; 401 gelince UI thread'de çağrılır. */
    public interface OnUnauthorizedListener {
        void onUnauthorized();
    }
    private static volatile OnUnauthorizedListener unauthorizedListener;
    public static void setOnUnauthorizedListener(OnUnauthorizedListener l) {
        unauthorizedListener = l;
    }

    // Emülatör için 10.0.2.2 (host localhost), gerçek telefon için Tailscale IP
    private static final String BASE_URL_EMULATOR  = "http://10.0.2.2:8081/";
    private static final String BASE_URL_TAILSCALE = "http://100.76.3.112:8081/";

    public static final String BASE_URL = isEmulator() ? BASE_URL_EMULATOR : BASE_URL_TAILSCALE;

    static {
        Log.d("ApiClient", "BASE_URL seçildi: " + BASE_URL
                + " | HARDWARE=" + android.os.Build.HARDWARE
                + " | PRODUCT=" + android.os.Build.PRODUCT);
    }

    private static boolean isEmulator() {
        return android.os.Build.HARDWARE.equals("goldfish")
                || android.os.Build.HARDWARE.equals("ranchu")
                || android.os.Build.FINGERPRINT.contains("generic")
                || android.os.Build.PRODUCT.startsWith("sdk")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK");
    }

    private static Retrofit publicRetrofit;
    private static Retrofit authRetrofit;
    private static String cachedToken;

    private static OkHttpClient buildPublicClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
    }

    /** Public (unauthenticated) Retrofit instance — for auth endpoints */
    public static Retrofit getInstance() {
        if (publicRetrofit == null) {
            publicRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(buildPublicClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return publicRetrofit;
    }

    /** Authenticated Retrofit instance — injects Bearer token into every request */
    public static Retrofit getAuthInstance(String token) {
        if (authRetrofit == null || !token.equals(cachedToken)) {
            cachedToken = token;

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request req = chain.request().newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        okhttp3.Response response = chain.proceed(req);
                        if (response.code() == 401) {
                            OnUnauthorizedListener l = unauthorizedListener;
                            if (l != null) l.onUnauthorized();
                        }
                        return response;
                    })
                    .addInterceptor(logging)
                    .build();

            authRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }

    /** Authenticated OkHttpClient — used for SSE (EventSource) streaming */
    public static OkHttpClient getAuthOkHttpClient(String token) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .header("Accept", "text/event-stream")
                            .build();
                    return chain.proceed(req);
                })
                .addInterceptor(logging)
                .build();
    }

    /** Call when token changes to force recreation of auth client */
    public static void invalidateAuthCache() {
        authRetrofit = null;
        cachedToken = null;
    }
}
