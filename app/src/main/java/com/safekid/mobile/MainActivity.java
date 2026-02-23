package com.safekid.mobile;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.safekid.mobile.databinding.ActivityMainBinding;
import com.safekid.mobile.session.SessionManager;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        setupBottomNav();

        if (savedInstanceState == null) {
            if (sessionManager.isLoggedIn()) {
                if (sessionManager.isParent()) {
                    navigateToParentHome();
                } else if (sessionManager.isChild()) {
                    navigateToChildActive();
                } else {
                    navigateToLogin();
                }
            } else {
                navigateToLogin();
            }
        }
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────────

    private void setupBottomNav() {
        binding.bottomNav.inflateMenu(R.menu.menu_bottom_nav);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new ParentHomeFragment(), false);
                return true;
            } else if (id == R.id.nav_map) {
                loadFragment(new ParentMapFragment(), false);
                return true;
            } else if (id == R.id.nav_alerts) {
                loadFragment(new AiAlertsFragment(), false);
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment(), false);
                return true;
            }
            return false;
        });
    }

    // ── Fragment Navigation ───────────────────────────────────────────────────

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction tx = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }

    public void showBottomNav(boolean show) {
        binding.bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ── High-level navigation helpers ─────────────────────────────────────────

    public void navigateToLogin() {
        showBottomNav(false);
        getSupportFragmentManager()
                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new LoginFragment(), false);
    }

    public void navigateToParentHome() {
        showBottomNav(true);
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        getSupportFragmentManager()
                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new ParentHomeFragment(), false);
    }

    public void navigateToChildActive() {
        showBottomNav(false);
        getSupportFragmentManager()
                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new ChildActiveFragment(), false);
    }
}
