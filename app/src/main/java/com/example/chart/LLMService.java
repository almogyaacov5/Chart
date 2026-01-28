package com.example.chart;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class LLMService {

    private static final String PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions";
    // וודא שהמפתח כאן הוא המפתח הנכון והמעודכן שלך
    private static final String API_KEY = "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";

    private final OkHttpClient client = new OkHttpClient();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private Context context;

    // --- בנאי (Constructor) ---
    public LLMService(Context context) {
        this.context = context;
    }

    // --- ממשקים (Callbacks) ---

    // ממשק ישן (לשימוש פנימי או במקומות אחרים)
    public interface AnalysisCallback {
        void onAnalysisReceived(String analysis);
        void onError(String error);
    }

    // ממשק חדש ונוח לשימוש בפרגמנטים
    public interface LLMCallback {
        void onSuccess(String result);
        void onFailure(Throwable t);
    }

    // --- פונקציות ציבוריות ---

    /**
     * פונקציה גנרית לשליחת כל טקסט ל-AI וקבלת תשובה.
     * מתאימה לניתוח תיק, המלצות, וסיכומים.
     */
    public void generateContent(String prompt, LLMCallback callback) {
        // המרה ל-Callback הפנימי
        sendToPerplexity(prompt, new AnalysisCallback() {
            @Override
            public void onAnalysisReceived(String analysis) {
                callback.onSuccess(analysis);
            }

            @Override
            public void onError(String error) {
                callback.onFailure(new Exception(error));
            }
        });
    }

    // פונקציה לשאלות ספציפיות על מניה (מהקוד המקורי שלך)
    public void askQuestion(String symbol, String question, String context,
                            List<Float> closes, AnalysisCallback callback) {
        String prompt = String.format(
                "🚨 **שאלה על %s**\n\n" +
                        "שאלה: %s\n\n" +
                        "הקשר: %s\n" +
                        "נתוני מחירים אחרונים: %s\n\n" +
                        "**ענה בעברית בלבד, תמציתי ומקצועי:**",
                symbol, question, context,
                (closes != null && closes.size() > 5) ?
                        String.join(", ", closes.subList(0, 5).stream()
                                .map(c -> df.format(c))
                                .toArray(String[]::new)) : "לא זמין"
        );
        sendToPerplexity(prompt, callback);
    }

    // --- פונקציות פרטיות ---

    private void sendToPerplexity(String prompt, AnalysisCallback callback) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "sonar-pro"); // מודל חכם לניתוח פיננסי
            requestBody.put("messages", new JSONArray().put(
                    new JSONObject()
                            .put("role", "user")
                            .put("content", prompt)
            ));
            requestBody.put("max_tokens", 1000); // מספיק מקום לתשובה מפורטת
            requestBody.put("temperature", 0.2); // יצירתיות נמוכה לטובת דיוק
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
}
