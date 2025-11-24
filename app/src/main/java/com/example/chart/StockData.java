package com.example.chart;

public class StockData {
    public String symbol;
    public float buyPrice;
    public float currentPrice;
    public float changePercent; // שינוי יומי באחוזים
    public double sellPrice;

    // קונסטרוקטור ריק בשביל Firebase
    public StockData() {}

    // קונסטרוקטור מלא - 4 פרמטרים
    public StockData(String symbol, float buyPrice, float currentPrice, float changePercent) {
        this.symbol = symbol;
        this.buyPrice = buyPrice;
        this.currentPrice = currentPrice;
        this.changePercent = changePercent;
    }

    // קונסטרוקטור חלקי - 3 פרמטרים (שינוי יומי מאופס)
    public StockData(String symbol, float buyPrice, float currentPrice) {
        this.symbol = symbol;
        this.buyPrice = buyPrice;
        this.currentPrice = currentPrice;
        this.changePercent = 0;
    }
}