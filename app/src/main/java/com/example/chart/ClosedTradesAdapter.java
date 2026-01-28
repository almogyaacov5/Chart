package com.example.chart;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // הוספנו כפתור
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClosedTradesAdapter extends RecyclerView.Adapter<ClosedTradesAdapter.ViewHolder> {

    // ממשק לתקשורת עם הפרגמנט
    public interface OnTradeEditListener {
        void onEditTrade(StockData trade);
    }

    private List<StockData> closedTrades;
    private OnTradeEditListener listener; // המאזין שלנו

    // בנאי מעודכן שמקבל גם את המאזין
    public ClosedTradesAdapter(List<StockData> closedTrades, OnTradeEditListener listener) {
        this.closedTrades = closedTrades;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // הנחה: בקובץ item_closed_trade.xml יש כפתור עם ID: btnEditTrade
        // אם אין, תצטרך להוסיף אותו ל-XML (ראה הערה למטה)
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_closed_trade, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockData trade = closedTrades.get(position);

        holder.symbolText.setText(trade.symbol);

        double buyPrice = trade.buyPrice; // התיקון הקודם (buyPrice)
        double sellPrice = trade.sellPrice;

        holder.sellPriceView.setText(String.format("Sold: $%.2f", sellPrice));
        holder.priceText.setText(String.format("Bought: $%.2f", buyPrice));

        // חישוב P&L
        double pnlPercent = 0;
        if (buyPrice != 0) {
            pnlPercent = ((sellPrice - buyPrice) / buyPrice) * 100;
        }

        String pnlString = String.format("%.2f%%", pnlPercent);
        if (pnlPercent > 0) {
            pnlString = "+" + pnlString;
            holder.percentText.setTextColor(Color.GREEN);
        } else if (pnlPercent < 0) {
            holder.percentText.setTextColor(Color.RED);
        } else {
            holder.percentText.setTextColor(Color.GRAY);
        }
        holder.percentText.setText(pnlString);

        // מאזין ללחיצה על כפתור העריכה
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditTrade(trade);
            }
        });
    }

    @Override
    public int getItemCount() {
        return closedTrades.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView symbolText, priceText, percentText, sellPriceView;
        ImageButton editButton; // משתנה לכפתור

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.tradeSymbolText);
            priceText = itemView.findViewById(R.id.tradeCurrentPrice);
            percentText = itemView.findViewById(R.id.tradeChangePercent);
            sellPriceView = itemView.findViewById(R.id.tradeSellingPrice);

            // וודא שבקובץ ה-XML הוספת ImageButton עם ה-ID הזה
            // אם לא הוספת, הקוד יקרוס כאן!
            // אם אתה רוצה פתרון זמני ללא שינוי XML, אפשר להשתמש ב-itemView.setOnLongClickListener
            editButton = itemView.findViewById(R.id.btnEditTrade);

            // פתרון חליפי אם אין כפתור ב-XML (הערה: הסר אם הוספת כפתור)
            /*
            itemView.setOnLongClickListener(v -> {
                 // לוגיקה לפתיחת עריכה בלחיצה ארוכה על כל השורה
                 return true;
            });
            */
        }
    }
}
