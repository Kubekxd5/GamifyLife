package com.example.gamifylife;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Import dla Handler
import android.os.Looper;  // Import dla Looper
import android.text.TextUtils;
import android.util.Log;   // Import dla Log
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.gamifylife.helpers.LocaleHelper;
import com.example.gamifylife.helpers.ThemeHelper;
import com.example.gamifylife.ui.settings.SettingsFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG_MAIN_ACTIVITY = "MainActivity"; // Tag dla logów

    private DrawerLayout drawerLayout;
    private FirebaseAuth mAuth;
    private AdView mAdViewBanner;
    private NavigationView navigationView; // Pole dla NavigationView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view); // Inicjalizuj pole navigationView
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        updateNavHeader();

        if (savedInstanceState == null) {
            Log.d(TAG_MAIN_ACTIVITY, "onCreate: savedInstanceState is null, preparing to load default fragment.");
            // Użyj Handler, aby opóźnić transakcję fragmentu, jeśli występują konflikty
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isFinishing() && !isDestroyed()) { // Sprawdź stan aktywności
                    Log.d(TAG_MAIN_ACTIVITY, "Loading default HomeFragment via Handler.");
                    Fragment defaultFragment = new HomeFragment();
                    loadFragment(defaultFragment, getString(R.string.menu_home));
                    if (navigationView != null) { // Sprawdź null dla navigationView
                        navigationView.setCheckedItem(R.id.nav_home);
                    } else {
                        Log.e(TAG_MAIN_ACTIVITY, "NavigationView is null when trying to setCheckedItem in Handler.");
                    }
                } else {
                    Log.w(TAG_MAIN_ACTIVITY, "Activity is finishing/destroyed, not loading default fragment.");
                }
            });
        }

        mAdViewBanner = findViewById(R.id.adViewBanner);
        if (mAdViewBanner != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdViewBanner.loadAd(adRequest);
        } else {
            Log.e(TAG_MAIN_ACTIVITY, "AdViewBanner not found in layout!");
        }

        // Widget Update (rozważ, kiedy to faktycznie potrzebne)
        // Context context = getApplicationContext();
        // AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        // ComponentName thisWidget = new ComponentName(context, GamifyLifeWidgetProvider.class);
        // int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        // if (appWidgetIds != null && appWidgetIds.length > 0) { // Sprawdź, czy są jakieś widgety
        //     Intent intent = new Intent(context, GamifyLifeWidgetProvider.class);
        //     intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        //     intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        //     context.sendBroadcast(intent);
        // }
    }

    private void updateNavHeader() {
        if (navigationView == null) { // Sprawdź, czy navigationView zostało zainicjalizowane
            Log.e(TAG_MAIN_ACTIVITY, "updateNavHeader: NavigationView is null.");
            return;
        }
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) {
            Log.e(TAG_MAIN_ACTIVITY, "updateNavHeader: HeaderView is null.");
            return;
        }
        TextView navHeaderUserEmail = headerView.findViewById(R.id.textViewLoggedInUserName);
        Button navHeaderLogoutButton = headerView.findViewById(R.id.buttonLogout);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (navHeaderUserEmail != null) {
                navHeaderUserEmail.setText(currentUser.getEmail());
            } else {
                Log.w(TAG_MAIN_ACTIVITY, "updateNavHeader: navHeaderUserEmail TextView not found.");
            }
            if (navHeaderLogoutButton != null) {
                navHeaderLogoutButton.setOnClickListener(v -> logoutUser());
            } else {
                Log.w(TAG_MAIN_ACTIVITY, "updateNavHeader: navHeaderLogoutButton not found.");
            }
        } else {
            Log.w(TAG_MAIN_ACTIVITY, "updateNavHeader: currentUser is null, navigating to login.");
            navigateToLogin();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        String title = "";
        Fragment selectedFragment = null;

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            title = getString(R.string.menu_home);
        } else if (itemId == R.id.nav_achievements) {
            selectedFragment = new AchievementsFragment();
            title = getString(R.string.title_achievements_fragment);
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
            title = getString(R.string.menu_settings);
        /*} else if (itemId == R.id.nav_slideshow) {
            title = getString(R.string.menu_slideshow);
            Toast.makeText(this, getString(R.string.slideshow_not_implemented_toast), Toast.LENGTH_SHORT).show();*/
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment, title);
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void loadFragment(Fragment fragment, String title) {
        if (fragment != null && !isFinishing() && !isDestroyed()) { // Sprawdź stan aktywności
            try {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                // fragmentTransaction.addToBackStack(null); // Opcjonalnie, jeśli chcesz nawigację wstecz między fragmentami
                fragmentTransaction.commit(); // Użyj commit(), commitAllowingStateLoss() tylko w ostateczności

                if (getSupportActionBar() != null && !TextUtils.isEmpty(title)) {
                    getSupportActionBar().setTitle(title);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG_MAIN_ACTIVITY, "Error committing fragment transaction: " + title, e);
                // Można spróbować commitAllowingStateLoss() jako ostateczność, ale lepiej zrozumieć przyczynę
                // FragmentManager fragmentManager = getSupportFragmentManager();
                // FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                // fragmentTransaction.replace(R.id.fragment_container, fragment);
                // fragmentTransaction.commitAllowingStateLoss();
            }
        } else {
            Log.w(TAG_MAIN_ACTIVITY, "Cannot load fragment: fragment is null or activity is finishing/destroyed.");
        }
    }

    // ... (reszta metod: onBackPressed, onCreateOptionsMenu, showLanguageSelectionDialog, onOptionsItemSelected, etc.)
    // Upewnij się, że wszystkie Toasty w tych metodach używają string resources.

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(MainActivity.this, getString(R.string.main_logged_out), Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        if (mAdViewBanner != null) {
            mAdViewBanner.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdViewBanner != null) {
            mAdViewBanner.resume();
        }
        // ... (reszta kodu onResume, np. updateNavHeader)
    }

    @Override
    protected void onDestroy() {
        if (mAdViewBanner != null) {
            mAdViewBanner.destroy();
        }
        super.onDestroy();
    }
}