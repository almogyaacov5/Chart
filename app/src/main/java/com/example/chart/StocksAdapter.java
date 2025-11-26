package com.example.chart;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import org.json.JSONObject;
import java.io.IOException;

public class StocksAdapter extends RecyclerView.Adapter<StocksAdapter.StockViewHolder> {

    public interface OnStockClickListener {
        void onStockClick(String symbol);
        void onStockDelete(String symbol, double sellPrice); //מעביר גם את מחיר הסגירה
    }

    private List<StockData> stocks;
    private final OnStockClickListener listener;
    private final OkHttpClient client = new OkHttpClient();

    public StocksAdapter(List<StockData> stocks, OnStockClickListener listener) {
        this.stocks = stocks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_portfolio, parent, false);
        return new StockViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        StockData stock = stocks.get(position);
        holder.symbolText.setText(stock.symbol);
        holder.buyPriceText.setText("Buy Price: " + stock.buyPrice);

        fetchCurrentPrice(stock.symbol, new PriceCallback() {
            @Override
            public void onPriceReceived(float price) {
                holder.currentPriceText.post(() -> holder.currentPriceText.setText("Current Price: " + price));
                float percentChange = (stock.buyPrice != 0f) ? ((price - stock.buyPrice) / stock.buyPrice * 100f) : 0f;
                holder.percentChangeText.post(() ->
                        holder.percentChangeText.setText("Change: " + String.format("%.2f", percentChange) + "%"));
            }
            @Override
            public void onError(Exception e) {
                holder.currentPriceText.post(() -> holder.currentPriceText.setText("Error"));
                holder.percentChangeText.post(() -> holder.percentChangeText.setText("Change: ?"));
            }
        });

        // כפתור סגירה עם דיאלוג מחיר
        holder.deleteButton.setOnClickListener(view -> {
            showSellPriceDialog(view.getContext(), stock.symbol);
        });

        holder.itemView.setOnClickListener(view -> listener.onStockClick(stock.symbol));
    }

    @Override
    public int getItemCount() {
        return stocks != null ? stocks.size() : 0;
    }

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

    public void refreshPrices() {
        notifyDataSetChanged();
    }

    // דיאלוג קלט למחיר סגירה
    private void showSellPriceDialog(Context context, String symbol) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("הזן מחיר סגירה ל-" + symbol);
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);
        builder.setPositiveButton("סגור טרייד", (dialog, which) -> {
            String priceStr = input.getText().toString();
            double sellPrice = priceStr.isEmpty() ? 0 : Double.parseDouble(priceStr);
            listener.onStockDelete(symbol, sellPrice);
        });
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
