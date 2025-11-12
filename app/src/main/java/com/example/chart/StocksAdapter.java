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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

        fetchCurrentPrice(holder.itemView.getContext(), stock.symbol, new PriceCallback() {
            @Override
            public void onPriceReceived(float price) {
                holder.currentPriceText.post(() -> {
                    holder.currentPriceText.setText("מחיר נוכחי: " + price);

                    float percentChange = (stock.buyPrice != 0f)
                            ? ((price - stock.buyPrice) / stock.buyPrice * 100f)
                            : 0f;
                    holder.percentChangeText.setText("שינוי: " + String.format("%.2f", percentChange) + "%");
                });
            }

            @Override
            public void onError(Exception e) {
                holder.currentPriceText.post(() -> holder.currentPriceText.setText("שגיאה"));
                holder.percentChangeText.post(() -> holder.percentChangeText.setText("אין נתון"));
            }
        });

        holder.itemView.setOnClickListener(view -> listener.onStockClick(stock.symbol));
        holder.deleteButton.setOnClickListener(view -> listener.onStockDelete(stock.symbol));
    }

    @Override
    public int getItemCount() {
        return stocks != null ? stocks.size() : 0;
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

    // שליפה אסינכרונית של מחיר המניה מה-API
    private void fetchCurrentPrice(Context context, String symbol, PriceCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.twelvedata.com/price?symbol=" + symbol
                + "&apikey=0518811f0d394fa39842a8024a25c049";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { callback.onError(e); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception("API Error"));
                    return;
                }
                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("price")) {
                    float price = Float.parseFloat(json.get("price").getAsString());
                    callback.onPriceReceived(price);
                } else {
                    callback.onError(new Exception("No price in response"));
                }
            }
        });
    }

    public interface PriceCallback {
        void onPriceReceived(float price);
        void onError(Exception e);
    }
}
