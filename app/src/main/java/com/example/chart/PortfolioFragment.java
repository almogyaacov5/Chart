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
import java.util.ArrayList;
import java.util.List;

public class PortfolioFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnAddStockToPortfolio;
    private List<StockData> stocksList;
    private StocksAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_portfolio, container, false);

        recyclerView = v.findViewById(R.id.tradesRecyclerView);
        btnAddStockToPortfolio = v.findViewById(R.id.btnAddStockToPortfolio);

        stocksList = getDemoPortfolio(); // תעדכן פה טעינה מ־DB אם צריך

        adapter = new StocksAdapter(stocksList, new StocksAdapter.OnStockClickListener() {
            @Override
            public void onStockClick(String symbol) {}
            @Override
            public void onStockDelete(String symbol) {}
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAddStockToPortfolio.setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PortfolioAddStockFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }

    private List<StockData> getDemoPortfolio() {
        List<StockData> list = new ArrayList<>();
        list.add(new StockData("AAPL", 150, 0));
        list.add(new StockData("TSLA", 1000, 0));
        return list;
    }
}
