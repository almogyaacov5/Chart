package com.example.chart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PortfolioFragment extends Fragment {

    private RecyclerView recyclerView;
    private ImageButton btnRefreshPortfolio;
    private Button btnAddStockToPortfolio;
    // הגדרת משתנה לכפתור ההתנתקות
    private Button btnLogout;

    private List<StockData> stocksList;
    private StocksAdapter adapter;
    private DatabaseReference portfolioRef;
    private DatabaseReference closedTradesRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_portfolio, container, false);

        recyclerView = v.findViewById(R.id.tradesRecyclerView);
        btnAddStockToPortfolio = v.findViewById(R.id.btnAddStockToPortfolio);
        btnRefreshPortfolio = v.findViewById(R.id.btnRefreshPortfolio);
        // קישור הכפתור מה-XML
        btnLogout = v.findViewById(R.id.btnLogout);

        stocksList = new ArrayList<>();

        // uid של המשתמש המחובר
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // נתיבים לפי משתמש
        portfolioRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("portfolio-stocks");

        closedTradesRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("closed-trades");

        adapter = new StocksAdapter(stocksList, new StocksAdapter.OnStockClickListener() {
            @Override
            public void onStockClick(String symbol) {
                // אם תרצה לפתוח גרף מהפורטפוליו
            }

            @Override
            public void onStockDelete(String symbol, double sellPrice) {
                // קבלת הדאטה מהפורטפוליו, עדכון מחיר סגירה והעברה לטריידים סגורים
                portfolioRef.child(symbol).get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        StockData data = snapshot.getValue(StockData.class);
                        if (data != null) {
                            data.sellPrice = sellPrice; // מחיר סגירה שהמשתמש הזין
                            closedTradesRef.child(symbol).setValue(data);
                            portfolioRef.child(symbol).removeValue();
                        }
                    }
                });
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        portfolioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                stocksList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StockData data = ds.getValue(StockData.class);
                    if (data != null) {
                        stocksList.add(data);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnAddStockToPortfolio.setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PortfolioAddStockFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnRefreshPortfolio.setOnClickListener(view -> adapter.refreshPrices());

        // הוספת מאזין לכפתור ההתנתקות
        btnLogout.setOnClickListener(view -> {
            // התנתקות מ-Firebase
            FirebaseAuth.getInstance().signOut();

            // חזרה למסך ההתחברות (AuthLogin)
            Intent intent = new Intent(getActivity(), AuthLogin.class);
            // מחיקת היסטוריית המסכים כדי למנוע חזרה לאפליקציה בלחיצה על Back
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return v;
    }
}
