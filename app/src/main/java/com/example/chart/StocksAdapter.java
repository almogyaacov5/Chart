package com.example.chart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StocksAdapter extends RecyclerView.Adapter<StocksAdapter.StockViewHolder> {

    public interface OnStockClickListener {
        void onStockClick(String symbol);
        void onStockDelete(String symbol);
    }

    private List<StockData> stocks;
    private final OnStockClickListener listener;

    public StocksAdapter(List<StockData> stocks, OnStockClickListener listener) {
        this.stocks = stocks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        StockData stock = stocks.get(position);
        holder.symbolText.setText(stock.symbol);
        holder.buyPriceText.setText("מחיר קנייה: " + stock.buyPrice);
        holder.currentPriceText.setText("מחיר נוכחי: " + stock.currentPrice);

        // חישוב אחוז שינוי ממחיר הקנייה:
        float percentChange = 0;
        if (stock.buyPrice != 0) {
            percentChange = ((stock.currentPrice - stock.buyPrice) / stock.buyPrice) * 100f;
        }
        holder.percentChangeText.setText("שינוי: " + String.format("%.2f", percentChange) + "%");

        holder.itemView.setOnClickListener(view -> listener.onStockClick(stock.symbol));
        holder.deleteButton.setOnClickListener(view -> listener.onStockDelete(stock.symbol));
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView symbolText, buyPriceText, currentPriceText, percentChangeText;
        ImageButton deleteButton;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.stockSymbolText);
            buyPriceText = itemView.findViewById(R.id.stockBuyPriceText);
            currentPriceText = itemView.findViewById(R.id.stockCurrentPriceText);
            percentChangeText = itemView.findViewById(R.id.stockPercentChangeText);
            deleteButton = itemView.findViewById(R.id.btnDeleteStock);
        }
    }
}
