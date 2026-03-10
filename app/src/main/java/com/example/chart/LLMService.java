package com.example.chart;

import android.os.Handler;
import android.os.Looper;

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
import java.util.concurrent.TimeUnit;

public class LLMService {

    // Gemini API (Models API)
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent";
    private static final String API_KEY = "AIzaSyCAsVHfPVd9GpkgbzhY0lUH7T12dZAdfnQ";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final DecimalFormat df = new DecimalFormat("#.##");

    public interface AnalysisCallback {
        void onAnalysisReceived(String analysis);
        void onError(String error);
    }

    public interface LLMCallback {
        void onSuccess(String result);
        void onFailure(Throwable t);
    }

    public LLMService() { }

    // לשימוש כללי
    public void generateContent(String prompt, LLMCallback callback) {
        sendToGemini(prompt, new AnalysisCallback() {
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

    // לשאלה על מניה
    public void askQuestion(String symbol, String question, String context,
                            List<Float> closes, AnalysisCallback callback) {
        String pricesStr = "לא זמין";
        if (closes != null && closes.size() >= 2) {
            String[] formatted = closes.subList(0, Math.min(5, closes.size()))
                    .stream()
                    .map(c -> df.format(c))
                    .toArray(String[]::new);
            pricesStr = String.join(", ", formatted);
        }

        String prompt = String.format(
                "אתה יועץ פיננסי מקצועי. ענה תמיד בעברית, תמציתי וברור.\n\n" +
                        "שאלה על מניה %s:\n\n" +
                        "שאלה: %s\n\n" +
                        "הקשר: %s\n" +
                        "מחירי סגירה אחרונים: %s\n\n" +
                        "ענה בעברית בלבד, תמציתי ומקצועי.",
                symbol, question, context, pricesStr
        );

        sendToGemini(prompt, callback);
    }

    // --- קריאה ל‑Gemini ---

    private void sendToGemini(String prompt, AnalysisCallback callback) {
        JSONObject requestBody = new JSONObject();
        try {
            // פורמט Gemini: { contents: [ { parts: [ { text: "..." } ] } ] }
            JSONObject part = new JSONObject().put("text", prompt);
            JSONArray parts = new JSONArray().put(part);
            JSONObject content = new JSONObject().put("parts", parts);
            JSONArray contents = new JSONArray().put(content);

            requestBody.put("contents", contents);
        } catch (Exception e) {
            notifyError(callback, "שגיאה בבניית הבקשה: " + e.getMessage());
            return;
        }

        String urlWithKey = GEMINI_API_URL + "?key=" + API_KEY;

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(urlWithKey)
                .addHeader("Accept", "application/json")
                .post(body)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                notifyError(callback, "שגיאת רשת: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    notifyError(callback, "שגיאת API " + response.code() + ": " + responseBody);
                    return;
                }

                try {
                    // Gemini מחזיר: { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray candidates = json.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        notifyError(callback, "לא התקבלה תשובה מה‑AI");
                        return;
                    }

                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length(); i++) {
                        sb.append(parts.getJSONObject(i).optString("text", ""));
                    }
                    String result = sb.toString().trim();

                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onAnalysisReceived(result));

                } catch (Exception e) {
                    notifyError(callback, "שגיאה בפענוח תגובה: " + e.getMessage());
                }
            }
        });
    }

    private void notifyError(AnalysisCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
    }
}
