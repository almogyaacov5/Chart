package com.example.chart;

public class StockWatchData {
    public String symbol;          // הטיקר
    public float currentPrice;     // מחיר נוכחי
    public float dayChangePercent; // אחוז שינוי יומי

    public StockWatchData() {} // חובה ל-Firebase

    public StockWatchData(String symbol, float currentPrice, float dayChangePercent) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.dayChangePercent = dayChangePercent;
    }
}
