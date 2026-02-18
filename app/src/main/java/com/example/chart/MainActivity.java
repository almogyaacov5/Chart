package com.example.chart;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private RecyclerView drawerRecyclerView;
    private DrawerNavAdapter drawerAdapter;

    // --- Nav order prefs ---
    private static final String PREFS_NAME = "nav_prefs";
    private static final String KEY_NAV_ORDER = "nav_order"; // "nav_chart,nav_stocks,..."

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerRecyclerView = findViewById(R.id.drawerRecyclerView);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupDrawerList();

        if (savedInstanceState == null) {
            navigateTo(R.id.nav_chart);
        }
    }

    private void setupDrawerList() {
        drawerRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<NavDrawerItem> items = buildItemsFromSavedOrder();
        drawerAdapter = new DrawerNavAdapter(items, item -> {
            navigateTo(item.id);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        drawerRecyclerView.setAdapter(drawerAdapter);

        ItemTouchHelper.SimpleCallback cb =
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int from = viewHolder.getBindingAdapterPosition();
                        int to = target.getBindingAdapterPosition();
                        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;

                        Collections.swap(drawerAdapter.getItems(), from, to);
                        drawerAdapter.notifyItemMoved(from, to);
                        return true;
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);
                        saveOrderFromItems(drawerAdapter.getItems()); // שומר אחרי שסיימו לגרור
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        // אין swipe
                    }

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return true; // גרירה בלחיצה ארוכה
                    }
                };

        new ItemTouchHelper(cb).attachToRecyclerView(drawerRecyclerView);
    }

    private void navigateTo(int id) {
        Fragment selectedFragment = null;
        String title = "";

        if (id == R.id.nav_chart) {
            selectedFragment = new ChartFragment();
            title = "Chart";
        } else if (id == R.id.nav_stocks) {
            selectedFragment = new WatchlistFragment();
            title = "My Stocks";
        } else if (id == R.id.nav_portfolio) {
            selectedFragment = new PortfolioFragment();
            title = "Portfolio";
        } else if (id == R.id.nav_closed_trades) {
            selectedFragment = new ClosedTradesFragment();
            title = "Closed Trades";
        } else if (id == R.id.nav_simulator) {
            selectedFragment = new SimulatorFragment();
            title = "Simulator";
        }

        if (selectedFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            setTitle(title);
            drawerAdapter.setSelectedId(id);
        }
    }

    public void showChartWithSymbol(String symbol) {
        ChartFragment chartFragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putString("symbol", symbol);
        chartFragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chartFragment)
                .commit();

        setTitle("Chart");
        if (drawerAdapter != null) drawerAdapter.setSelectedId(R.id.nav_chart);
    }

    // -------------------------
    // Build items + persistence
    // -------------------------
    private List<NavDrawerItem> buildItemsFromSavedOrder() {
        String[] defaultKeys = new String[]{
                "nav_chart",
                "nav_stocks",
                "nav_portfolio",
                "nav_closed_trades",
                "nav_simulator"
        };

        String[] keys = loadKeysOrDefault(defaultKeys);

        List<NavDrawerItem> result = new ArrayList<>();
        for (String key : keys) {
            int id = navIdFromKey(key);
            if (id != 0) {
                result.add(new NavDrawerItem(id, key, titleForNavId(id)));
            }
        }
        return result;
    }

    private String[] loadKeysOrDefault(String[] defaultKeys) {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = sp.getString(KEY_NAV_ORDER, null);
        if (saved == null || saved.trim().isEmpty()) return defaultKeys;

        String[] parts = saved.split(",");
        HashSet<String> allowed = new HashSet<>();
        Collections.addAll(allowed, defaultKeys);

        List<String> out = new ArrayList<>();
        HashSet<String> used = new HashSet<>();

        for (String p : parts) {
            String k = p.trim();
            if (allowed.contains(k) && !used.contains(k)) {
                used.add(k);
                out.add(k);
            }
        }

        for (String k : defaultKeys) {
            if (!used.contains(k)) out.add(k);
        }

        return out.toArray(new String[0]);
    }

    private void saveOrderFromItems(List<NavDrawerItem> items) {
        List<String> keys = new ArrayList<>();
        for (NavDrawerItem it : items) keys.add(it.key);

        String joined = android.text.TextUtils.join(",", keys);
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sp.edit().putString(KEY_NAV_ORDER, joined).apply();
    }

    private int navIdFromKey(String key) {
        if ("nav_chart".equals(key)) return R.id.nav_chart;
        if ("nav_stocks".equals(key)) return R.id.nav_stocks;
        if ("nav_portfolio".equals(key)) return R.id.nav_portfolio;
        if ("nav_closed_trades".equals(key)) return R.id.nav_closed_trades;
        if ("nav_simulator".equals(key)) return R.id.nav_simulator;
        return 0;
    }

    private String titleForNavId(int id) {
        if (id == R.id.nav_chart) return "Chart";
        if (id == R.id.nav_stocks) return "My Stocks";
        if (id == R.id.nav_portfolio) return "Portfolio";
        if (id == R.id.nav_closed_trades) return "Closed Trades";
        if (id == R.id.nav_simulator) return "Simulator";
        return "Item";
    }

    // -------------------------
    // Notifications permission
    // -------------------------
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }


}
