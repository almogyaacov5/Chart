package com.example.chart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.WatchViewHolder> {

    public interface OnWatchStockClickListener {
        void onStockClick(String symbol);
        void onStockDelete(String symbol);
    }

    private List<StockWatchData> stocks;
    private final OnWatchStockClickListener listener;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final String API_KEY = "0518811f0d394fa39842a8024a25c049";

    public WatchlistAdapter(List<StockWatchData> stocks, OnWatchStockClickListener listener) {
        this.stocks = stocks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watchlist_stock, parent, false);
        return new WatchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchViewHolder holder, int position) {
        StockWatchData stock = stocks.get(position);
        holder.symbolText.setText(stock.symbol);

        fetchStockData(stock.symbol, "1day", new StockDataCallback() {
            @Override
            public void onDataReceived(float price, float dayChange) {
                holder.priceText.post(() -> holder.priceText.setText("Stock Price: " + price));
                holder.dayChangeText.post(() -> holder.dayChangeText.setText("Day Change: " + String.format("%.2f", dayChange) + "%"));
            }
            @Override
            public void onError(Exception e) {
                holder.priceText.post(() -> holder.priceText.setText("Stock Price: ?"));
                holder.dayChangeText.post(() -> holder.dayChangeText.setText("Day Change: ?"));
            }
        });

        holder.itemView.setOnClickListener(view -> listener.onStockClick(stock.symbol));
        holder.deleteButton.setOnClickListener(view -> listener.onStockDelete(stock.symbol));
    }

    @Override
    public int getItemCount() {
        return stocks != null ? stocks.size() : 0;
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

    // פונקציה חדשה שמבוססת על time_series לשינוי יומי ומחיר
    private void fetchStockData(String symbol, String interval, StockDataCallback callback) {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol +
                "&interval=" + interval + "&apikey=" + API_KEY + "&outputsize=2";
        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    if (json.has("values")) {
                        org.json.JSONArray arr = json.getJSONArray("values");
                        if (arr.length() >= 2) {
                            float lastClose = Float.parseFloat(arr.getJSONObject(0).getString("close"));
                            float prevClose = Float.parseFloat(arr.getJSONObject(1).getString("close"));
                            float dayChange = (lastClose - prevClose) / prevClose * 100f;
                            callback.onDataReceived(lastClose, dayChange);
                            return;
                        }
                    }
                    callback.onError(new Exception("לא נמצא מידע"));
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    public interface StockDataCallback {
        void onDataReceived(float price, float dayChange);
        void onError(Exception e);
    }
}
