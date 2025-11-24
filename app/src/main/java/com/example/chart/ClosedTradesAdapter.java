package com.example.chart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

public class ClosedTradesAdapter extends RecyclerView.Adapter<ClosedTradesAdapter.ViewHolder> {
    private List<StockData> closedTrades;
    private final OkHttpClient client = new OkHttpClient();

    public ClosedTradesAdapter(List<StockData> closedTrades) {
        this.closedTrades = closedTrades;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_closed_trade, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockData trade = closedTrades.get(position);
        holder.symbolText.setText(trade.symbol);
        holder.sellPriceView.setText("מחיר מכירה: " + trade.sellPrice);

        fetchCurrentPrice(trade.symbol, new PriceCallback() {
            @Override
            public void onPriceReceived(float price) {
                holder.priceText.post(() -> holder.priceText.setText("מחיר נוכחי: " + price));
                float percentChange = (trade.sellPrice != 0f) ? ((price - (float)trade.sellPrice) / (float)trade.sellPrice * 100f) : 0f;
                holder.percentText.post(() ->
                        holder.percentText.setText("שינוי יומי: " + String.format("%.2f", percentChange) + "%"));
            }
            @Override
            public void onError(Exception e) {
                holder.priceText.post(() -> holder.priceText.setText("שגיאה"));
                holder.percentText.post(() -> holder.percentText.setText("שינוי יומי: ?"));
            }
        });
    }

    @Override
    public int getItemCount() {
        return closedTrades.size();
    }

    // קריאה ל-API
    private void fetchCurrentPrice(String symbol, PriceCallback callback) {
        String apiKey = "0518811f0d394fa39842a8024a25c049";
        String url = "https://api.twelvedata.com/price?symbol=" + symbol + "&apikey=" + apiKey;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject obj = new JSONObject(body);
                    float price = Float.parseFloat(obj.getString("price"));
                    callback.onPriceReceived(price);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    public interface PriceCallback {
        void onPriceReceived(float price);
        void onError(Exception e);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView symbolText, priceText, percentText, sellPriceView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.tradeSymbolText);
            priceText = itemView.findViewById(R.id.tradeCurrentPrice);
            percentText = itemView.findViewById(R.id.tradeChangePercent);
            sellPriceView = itemView.findViewById(R.id.tradeSellingPrice);
        }
    }
}
