package com.example.chart;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WatchlistFragment extends Fragment {

    private static final String ALERT_CHANNEL_ID = "stock_price_alerts";

    private List<StockWatchData> watchlist;
    private WatchlistAdapter adapter;
    private DatabaseReference watchlistRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_watchlist, container, false);

        createNotificationChannel();

        // משתמש מחובר
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "צריך להתחבר כדי להשתמש ברשימת מעקב", Toast.LENGTH_SHORT).show();
            return v;
        }

        String uid = user.getUid();

        // Watchlist לפי משתמש: watchlist-stocks/{uid}/{symbol}
        watchlistRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("watchlist-stocks");


        watchlist = new ArrayList<>();

        // Listener אנונימי: לא צריך שה-Fragment יממש interface, ולכן אין שגיאות קומפילציה
        WatchlistAdapter.OnWatchStockClickListener listener = new WatchlistAdapter.OnWatchStockClickListener() {
            @Override
            public void onStockClick(String symbol) {
                openChart(symbol);
            }

            @Override
            public void onStockDelete(String symbol) {
                deleteStock(symbol);
            }

            // אם אצלך בממשק קיימת מתודה כזו – היא תעבוד.
            // אם לא קיימת – היא פשוט תהיה "מתודה נוספת" במחלקה האנונימית ולא תפריע.
            public void onAlertStateChanged(String symbol, boolean triggered) {
                if (watchlistRef == null) return;
                watchlistRef.child(symbol).child("alertTriggered").setValue(triggered);
            }

            // אותו רעיון: עובד אם קיים אצלך, ולא מפריע אם לא.
            public void onSetPriceAlert(StockWatchData stock) {
                showPriceAlertDialog(stock);
            }
        };

        adapter = new WatchlistAdapter(watchlist, listener);

        RecyclerView recyclerView = v.findViewById(R.id.watchlistRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        EditText stockInput = v.findViewById(R.id.stockInput);
        Button addStockBtn = v.findViewById(R.id.addStockBtn);
        ImageButton btnRefreshWatchlist = v.findViewById(R.id.btnRefreshWatchlist);

        addStockBtn.setOnClickListener(view -> {
            String symbol = stockInput.getText().toString().trim().toUpperCase();
            if (symbol.isEmpty()) {
                Toast.makeText(getContext(), "הזן סימבול", Toast.LENGTH_SHORT).show();
                return;
            }

            StockWatchData stock = new StockWatchData(symbol, 0f, 0f);
            watchlistRef.child(symbol).setValue(stock);
            stockInput.setText("");
        });

        btnRefreshWatchlist.setOnClickListener(view -> {
            adapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Watchlist refreshed", Toast.LENGTH_SHORT).show();
        });

        watchlistRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                watchlist.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StockWatchData data = ds.getValue(StockWatchData.class);
                    if (data != null) watchlist.add(data);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "שגיאה בטעינת רשימת המעקב", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    private void openChart(String symbol) {
        ChartFragment chartFragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putString("symbol", symbol);
        chartFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, chartFragment)
                .addToBackStack(null)
                .commit();
    }

    private void deleteStock(String symbol) {
        if (watchlistRef == null) return;
        watchlistRef.child(symbol).removeValue();
        Toast.makeText(getContext(), "המניה הוסרה", Toast.LENGTH_SHORT).show();
    }

    private void showPriceAlertDialog(StockWatchData stock) {
        if (stock == null || stock.symbol == null) return;

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("לדוגמה: 150.5");

        new AlertDialog.Builder(requireContext())
                .setTitle("התראת מחיר: " + stock.symbol)
                .setMessage("הזן מחיר יעד. (השמירה תהיה תחת המשתמש הנוכחי)")
                .setView(input)
                .setPositiveButton("שמור", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    try {
                        float target = Float.parseFloat(value);

                        // שומר שדות התראה במסד (גם אם הם לא קיימים ב-StockWatchData, זה בסדר במסד)
                        watchlistRef.child(stock.symbol).child("alertTargetPrice").setValue(target);
                        watchlistRef.child(stock.symbol).child("alertEnabled").setValue(true);
                        watchlistRef.child(stock.symbol).child("alertTriggered").setValue(false);

                        Toast.makeText(getContext(), "נשמרה התראת מחיר", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "מספר לא תקין", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("כבה", (dialog, which) -> {
                    watchlistRef.child(stock.symbol).child("alertEnabled").setValue(false);
                    watchlistRef.child(stock.symbol).child("alertTriggered").setValue(false);
                    Toast.makeText(getContext(), "ההתראה כובתה", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Stock price alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications when watchlist stocks cross your target price");

            NotificationManager nm =
                    (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }
}
