package com.example.chart;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class WatchlistFragment extends Fragment implements WatchlistAdapter.OnWatchStockClickListener {

    private List<StockWatchData> watchlist;
    private WatchlistAdapter adapter;
    private DatabaseReference watchlistRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_watchlist, container, false);

        watchlistRef = FirebaseDatabase.getInstance().getReference("watchlist-stocks");
        watchlist = new ArrayList<>();
        adapter = new WatchlistAdapter(watchlist, this);

        RecyclerView recyclerView = v.findViewById(R.id.watchlistRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        EditText stockInput = v.findViewById(R.id.stockInput);
        Button addStockBtn = v.findViewById(R.id.addStockBtn);
        ImageButton btnRefreshAll = v.findViewById(R.id.btnRefreshAll);

        addStockBtn.setOnClickListener(view -> {
            String symbol = stockInput.getText().toString().trim().toUpperCase();
            if (!symbol.isEmpty()) {
                StockWatchData stock = new StockWatchData(symbol, 0f, 0f);
                watchlistRef.child(symbol).setValue(stock);
                stockInput.setText("");
            }
        });

        btnRefreshAll.setOnClickListener(view -> {
            adapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "רשימת מעקב רועננה", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onStockClick(String symbol) {
        Toast.makeText(getContext(), "לחצת על " + symbol, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStockDelete(String symbol) {
        watchlistRef.child(symbol).removeValue();
        Toast.makeText(getContext(), "המניה הוסרה", Toast.LENGTH_SHORT).show();
    }
}
