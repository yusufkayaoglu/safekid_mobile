package com.safekid.mobile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.safekid.mobile.databinding.FragmentPremiumBinding;
import com.safekid.mobile.network.dto.SubscriptionStatusDto;
import com.safekid.mobile.session.SessionManager;
import com.safekid.mobile.viewmodel.ParentViewModel;

import java.util.Collections;
import java.util.List;

/**
 * Premium abonelik satın alma ekranı.
 *
 * Product ID'leri (Google Play Console'da oluşturulmalı):
 *   - safekid_premium_monthly   → Aylık abonelik
 *   - safekid_premium_yearly    → Yıllık abonelik (tasarruflu)
 *
 * Test için Google Play Console → Internal Testing → test hesabı ekle.
 */
public class PremiumFragment extends Fragment implements PurchasesUpdatedListener {

    private static final String TAG = "PremiumFragment";

    // Google Play Console'da tanımlayacağın product ID'leri
    public static final String PRODUCT_MONTHLY = "safekid_premium_monthly";
    public static final String PRODUCT_YEARLY = "safekid_premium_yearly";

    private FragmentPremiumBinding binding;
    private ParentViewModel viewModel;
    private SessionManager session;
    private BillingClient billingClient;

    private ProductDetails monthlyDetails;
    private ProductDetails yearlyDetails;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPremiumBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentViewModel.class);
        session = new SessionManager(requireContext());

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Halihazırda premium ise durumu göster
        if (session.isPremium()) {
            showAlreadyPremium();
        }

        setupClickListeners();
        observeViewModel();
        initBillingClient();
    }

    private void setupClickListeners() {
        binding.btnBuyMonthly.setOnClickListener(v -> launchPurchase(monthlyDetails));
        binding.btnBuyYearly.setOnClickListener(v -> launchPurchase(yearlyDetails));
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnBuyMonthly.setEnabled(!loading);
            binding.btnBuyYearly.setEnabled(!loading);
        });

        viewModel.getSubscriptionStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null && status.premium) {
                showAlreadyPremium();
                Toast.makeText(requireContext(),
                        "SafeKid Premium aktif! Tüm özellikler açıldı.",
                        Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    // ── Google Play Billing ───────────────────────────────────────────────────

    private void initBillingClient() {
        billingClient = BillingClient.newBuilder(requireContext())
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProducts();
                } else {
                    Log.e(TAG, "Billing bağlantısı başarısız: " + result.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing servisi kesildi — yeniden bağlanılıyor...");
                initBillingClient();
            }
        });
    }

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> products = List.of(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_YEARLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Ürün sorgusu başarısız: " + billingResult.getDebugMessage());
                return;
            }

            requireActivity().runOnUiThread(() -> {
                for (ProductDetails details : productDetailsList) {
                    if (PRODUCT_MONTHLY.equals(details.getProductId())) {
                        monthlyDetails = details;
                        updateMonthlyButton(details);
                    } else if (PRODUCT_YEARLY.equals(details.getProductId())) {
                        yearlyDetails = details;
                        updateYearlyButton(details);
                    }
                }
                binding.btnBuyMonthly.setEnabled(monthlyDetails != null);
                binding.btnBuyYearly.setEnabled(yearlyDetails != null);
            });
        });
    }

    private void updateMonthlyButton(ProductDetails details) {
        if (details.getSubscriptionOfferDetails() == null
                || details.getSubscriptionOfferDetails().isEmpty()) return;
        String price = details.getSubscriptionOfferDetails()
                .get(0).getPricingPhases().getPricingPhaseList()
                .get(0).getFormattedPrice();
        binding.btnBuyMonthly.setText("Aylık — " + price);
    }

    private void updateYearlyButton(ProductDetails details) {
        if (details.getSubscriptionOfferDetails() == null
                || details.getSubscriptionOfferDetails().isEmpty()) return;
        String price = details.getSubscriptionOfferDetails()
                .get(0).getPricingPhases().getPricingPhaseList()
                .get(0).getFormattedPrice();
        binding.btnBuyYearly.setText("Yıllık — " + price + " (En iyi fiyat)");
    }

    private void launchPurchase(@Nullable ProductDetails details) {
        if (details == null) {
            Toast.makeText(requireContext(),
                    "Ürün bilgisi yüklenemedi. Lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (details.getSubscriptionOfferDetails() == null
                || details.getSubscriptionOfferDetails().isEmpty()) return;

        String offerToken = details.getSubscriptionOfferDetails().get(0).getOfferToken();

        BillingFlowParams.ProductDetailsParams productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build();

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productParams))
                .build();

        billingClient.launchBillingFlow(requireActivity(), flowParams);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult result, @Nullable List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Kullanıcı satın almayı iptal etti");
        } else {
            Log.e(TAG, "Satın alma hatası: " + result.getDebugMessage());
            Toast.makeText(requireContext(),
                    "Satın alma başarısız: " + result.getDebugMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;

        // Backend'e doğrulat — backend onaylarsa premium aktive eder
        String productId = purchase.getProducts().isEmpty() ? "" : purchase.getProducts().get(0);
        viewModel.verifyPurchase(
                purchase.getPurchaseToken(),
                productId,
                requireContext().getPackageName());

        // Acknowledge (Google Play zorunlu tutar; backend'den gelen onayın ardından yapılabilir)
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.acknowledgePurchase(ackParams, ackResult ->
                    Log.d(TAG, "Acknowledge: " + ackResult.getResponseCode()));
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private void showAlreadyPremium() {
        binding.layoutPlans.setVisibility(View.GONE);
        binding.layoutPremiumActive.setVisibility(View.VISIBLE);
        String expiresAt = session.getSubscriptionExpiresAt();
        if (expiresAt != null) {
            binding.tvPremiumExpiry.setText("Abonelik bitiş: " + expiresAt);
            binding.tvPremiumExpiry.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (billingClient != null) billingClient.endConnection();
        binding = null;
    }
}
