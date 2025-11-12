package com.example.chart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PortfolioFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnAddStockToPortfolio;
    private List<StockData> stocksList = new ArrayList<>();
    private StocksAdapter adapter;
    private DatabaseReference stocksRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_portfolio, container, false);

        recyclerView = v.findViewById(R.id.tradesRecyclerView); // או שם שונה אם השתמשת ב-id אחר
        btnAddStockToPortfolio = v.findViewById(R.id.btnAddStockToPortfolio);

        adapter = new StocksAdapter(stocksList, new StocksAdapter.OnStockClickListener() {
            @Override
            public void onStockClick(String symbol) {
                // כאן אפשר להוסיף לוגיקה להציג פירוט/גרף וכו'
            }
            @Override
            public void onStockDelete(String symbol) {
                stocksRef.child(symbol).removeValue();
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        stocksRef = FirebaseDatabase.getInstance().getReference("portfolio-stocks");

        loadPortfolioStocks();

        btnAddStockToPortfolio.setOnClickListener(view -> {
            // פותח את דף ההוספה החדש
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PortfolioAddStockFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }

    private void loadPortfolioStocks() {
        stocksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                stocksList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StockData data = ds.getValue(StockData.class);
                    if (data != null) stocksList.add(data);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // טיפול בשגיאות (ניתן להציג Toast אם נדרש)
            }
        });
    }
}
