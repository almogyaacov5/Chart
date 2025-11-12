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
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class WatchlistFragment extends Fragment implements WatchlistAdapter.OnWatchStockClickListener {

    private List<StockWatchData> watchlist;
    private WatchlistAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_watchlist, container, false);

        watchlist = new ArrayList<>();
        adapter = new WatchlistAdapter(watchlist, this);

        RecyclerView recyclerView = v.findViewById(R.id.watchlistRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // דוגמה: הוספת כמה מניות למעקב (תמחק או תביא מנתונים אמיתיים)
        watchlist.add(new StockWatchData("AAPL", 158.5f, 1.12f));
        watchlist.add(new StockWatchData("TSLA", 234.1f, -2.45f));
        adapter.notifyDataSetChanged();

        return v;
    }

    @Override
    public void onStockClick(String symbol) {
        Toast.makeText(getContext(), "לחצת על " + symbol, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStockDelete(String symbol) {
        int pos = -1;
        for (int i = 0; i < watchlist.size(); i++) {
            if (watchlist.get(i).symbol.equals(symbol)) {
                pos = i;
                break;
            }
        }
        if (pos != -1) {
            watchlist.remove(pos);
            adapter.notifyItemRemoved(pos);
            Toast.makeText(getContext(), "נמחקה מניה: " + symbol, Toast.LENGTH_SHORT).show();
        }
    }
}
