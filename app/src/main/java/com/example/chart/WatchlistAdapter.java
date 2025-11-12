package com.example.chart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.WatchViewHolder> {
    public interface OnWatchStockClickListener {
        void onStockClick(String symbol);
        void onStockDelete(String symbol);
    }

    private List<StockWatchData> stocks;
    private OnWatchStockClickListener listener;

    public WatchlistAdapter(List<StockWatchData> stocks, OnWatchStockClickListener listener) {
        this.stocks = stocks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_watchlist_stock, parent, false);
        return new WatchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchViewHolder holder, int position) {
        StockWatchData stock = stocks.get(position);
        holder.symbolText.setText(stock.symbol);
        holder.priceText.setText("שווי מניה: " + stock.currentPrice);
        holder.dayChangeText.setText("שינוי יומי: " + String.format("%.2f", stock.dayChangePercent) + "%");

        // מאזינים ללחיצה על כל שורה ו-איקס
        holder.itemView.setOnClickListener(view -> listener.onStockClick(stock.symbol));
        holder.deleteButton.setOnClickListener(view -> listener.onStockDelete(stock.symbol));
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    static class WatchViewHolder extends RecyclerView.ViewHolder {
        TextView symbolText, priceText, dayChangeText;
        ImageButton deleteButton;

        public WatchViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.stockSymbolText);
            priceText = itemView.findViewById(R.id.stockPriceText);
            dayChangeText = itemView.findViewById(R.id.stockDayChangeText);
            deleteButton = itemView.findViewById(R.id.btnDeleteStock);
        }
    }
}
