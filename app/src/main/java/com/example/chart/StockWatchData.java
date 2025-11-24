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

    public String getSymbol() {
        return symbol;
    }

    public float getCurrentPrice() {
        return currentPrice;
    }

    public float getDayChangePercent() {
        return dayChangePercent;
    }

    // ניתן גם להוסיף setters אם צריך:
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setCurrentPrice(float currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setDayChangePercent(float dayChangePercent) {
        this.dayChangePercent = dayChangePercent;
    }
}