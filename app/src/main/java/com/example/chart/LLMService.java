package com.example.chart;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class LLMService {
    private static final String PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions";
    private static final String API_KEY = "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";
    private final OkHttpClient client = new OkHttpClient();
    private final DecimalFormat df = new DecimalFormat("#.##");

    public interface AnalysisCallback {
        void onAnalysisReceived(String analysis);
        void onError(String error);
    }

    public void analyzeStock(String symbol, List<Float> closes, Context context, AnalysisCallback callback) {
        if (closes == null || closes.size() < 2) {
            callback.onError("נתונים לא מספיקים (צריך לפחות 2 נקודות)");
            return;
        }

        String prompt = buildTradingPrompt(symbol, closes);
        Log.d("LLMService", "Prompt: " + prompt.substring(0, Math.min(200, prompt.length())));

        if (prompt.trim().isEmpty()) {
            callback.onError("פרומפט ריק");
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "sonar-pro");
            requestBody.put("messages", new JSONArray().put(
                    new JSONObject()
                            .put("role", "user")
                            .put("content", prompt)
            ));
            requestBody.put("max_tokens", 800);
            requestBody.put("temperature", 0.1);
            requestBody.put("stream", false);
        } catch (Exception e) {
            callback.onError("שגיאה בבניית JSON: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(PERPLEXITY_API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(callback, "שגיאת רשת: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d("LLMService", "Status: " + response.code() + " | Response: " + responseBody.substring(0, 200));

                    if (!response.isSuccessful()) {
                        runOnUiThread(callback, "API Error " + response.code() + ": " + responseBody);
                        return;
                    }

                    JSONObject json = new JSONObject(responseBody);
                    JSONArray choices = json.getJSONArray("choices");
                    String analysis = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    runOnUiThread(callback, analysis.trim());
                } catch (Exception e) {
                    runOnUiThread(callback, "שגיאת תגובה: " + e.getMessage());
                }
            }
        });
    }

    private void runOnUiThread(AnalysisCallback callback, String result) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (result.startsWith("API Error") || result.startsWith("שגיאת")) {
                callback.onError(result);
            } else {
                callback.onAnalysisReceived(result);
            }
        });
    }

    // 🔥 פרומפט מלא ומסודר - רשימה בדיוק!
    private String buildTradingPrompt(String symbol, List<Float> closes) {
        float currentPrice = closes.get(0);
        float change1d = closes.size() > 1 ?
                ((closes.get(0) - closes.get(1)) / closes.get(1)) * 100 : 0;

        return String.format(symbol+"רשום לי שיר בעל 4 שורות על המניה");
//                "🚨 **%s - מחיר נוכחי: $%.2f** 🚨\n\n" +
//                        "נתונים:\n" +
//                        "• שינוי יומי: %.1f%%\n" +
//                        "• מספר נקודות: %d\n\n" +
//                        "**תענה בדיוק ברשימה הזו בלבד:**\n\n" +
//                        "🎯 **המלצה:** קנה / מכור / המתן\n" +
//                        "📊 **סיבה 1:** ...\n" +
//                        "📊 **סיבה 2:** ...\n" +
//                        "📊 **סיבה 3:** ...\n" +
//                        "🛑 **Stop Loss:** $%.2f\n" +
//                        "💰 **Take Profit 1:** $%.2f\n" +
//                        "💎 **Take Profit 2:** $%.2f\n" +
//                        "⚠️ **רמת סיכון:** נמוכה / בינונית / גבוהה\n\n" +
//                        "**רק הרשימה! ללא טקסט נוסף! עברית בלבד!**",
//                symbol, currentPrice, change1d, closes.size(),
//                currentPrice * 0.95f,  // Stop Loss -5%
//                currentPrice * 1.08f,  // TP1 +8%
//                currentPrice * 1.15f   // TP2 +15%
//        );
    }
}
