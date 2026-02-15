package com.example.chart;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChartFragment extends Fragment implements TimeFrameFragment.TimeFrameListener {

    private CandleStickChart candleStickChart;
    private LineChart lineChart;

    // היה EditText -> עכשיו AutoCompleteTextView
    private AutoCompleteTextView tickerInput;
    private Button btnLoad, btnTimeFrame, btnToggleChart, btnAIAnalysis;
    private ImageButton btnChartRefresh;
    private TextView timeFrameText, tickerText, priceText, changeText, currentPriceDisplay;
    private ProgressBar progressAI;

    private final OkHttpClient client = new OkHttpClient();
    private final String API_KEY = "0518811f0d394fa39842a8024a25c049";

    private String symbol = "NVDA";
    private String interval = "1day";
    private boolean isCandleStick = true;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private String latestQuery = "";

    private LLMService llmService;
    private final List<CandleEntry> currentEntries = new ArrayList<>();
    private final List<Float> fullCloses = new ArrayList<>();
    private float lastPrice = 0f;
    private boolean isManualSelection = false;


    // --- Autocomplete ---
    private ArrayAdapter<StockSuggestion> suggestionAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private Call symbolSearchCall;
    private static final long SEARCH_DEBOUNCE_MS = 100;

    public static class StockSuggestion {
        public final String symbol;
        public final String name;
        public final String exchange;

        public StockSuggestion(String symbol, String name, String exchange) {
            this.symbol = symbol;
            this.name = name;
            this.exchange = exchange;
        }

        @NonNull
        @Override
        public String toString() {
            String s = (symbol == null) ? "" : symbol;
            String n = (name == null) ? "" : name;
            String ex = (exchange == null) ? "" : exchange;

            if (n.isEmpty() && ex.isEmpty()) return s;
            if (ex.isEmpty()) return s + " — " + n;
            return s + " — " + n + " (" + ex + ")";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chart, container, false);

        candleStickChart = v.findViewById(R.id.stock_chart);
        lineChart = v.findViewById(R.id.line_chart);
        tickerInput = v.findViewById(R.id.ticker_input);

        btnLoad = v.findViewById(R.id.btnLoad);
        btnTimeFrame = v.findViewById(R.id.btnSelectTimeFrame);
        btnToggleChart = v.findViewById(R.id.btnToggleChart);
        btnChartRefresh = v.findViewById(R.id.btnChartRefresh);
        btnAIAnalysis = v.findViewById(R.id.btnAIAnalysis);

        progressAI = v.findViewById(R.id.progressAI);
        priceText = v.findViewById(R.id.priceText);
        changeText = v.findViewById(R.id.changeText);
        timeFrameText = v.findViewById(R.id.timeFrameText);
        tickerText = v.findViewById(R.id.tickerText);
        currentPriceDisplay = v.findViewById(R.id.currentPriceDisplay);

        llmService = new LLMService(getContext());
        if (progressAI != null) progressAI.setVisibility(View.GONE);
        if (currentPriceDisplay != null) currentPriceDisplay.setVisibility(View.GONE);

        if (getArguments() != null && getArguments().containsKey("symbol")) {
            symbol = getArguments().getString("symbol", symbol);
            if (tickerInput != null) tickerInput.setText(symbol);
        }

        if (getActivity() != null) getActivity().setTitle("Chart: " + symbol);

        setupAutoComplete();
        setupClickListeners();

        fetchStockData(symbol, interval);
        return v;
    }

    private void setupAutoComplete() {
        if (tickerInput == null) return;

        suggestionAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        );

        tickerInput.setAdapter(suggestionAdapter);
        tickerInput.setThreshold(1);

        tickerInput.setOnItemClickListener((parent, view, position, id) -> {
            StockSuggestion sel = suggestionAdapter.getItem(position);
            if (sel == null || sel.symbol == null || sel.symbol.trim().isEmpty()) return;

            String picked = sel.symbol.trim().toUpperCase(Locale.US);

            // 🔥 מעלים את הדגל כדי לחסום את ה-TextWatcher
            isManualSelection = true;
            tickerInput.setText(picked);
            tickerInput.setSelection(picked.length());
            tickerInput.dismissDropDown(); // סוגר את הרשימה מיד

            setSymbolAndLoad(picked);

            // מורידים את הדגל אחרי עיכוב קצר
            tickerInput.postDelayed(() -> isManualSelection = false, 300);
        });

        tickerInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isManualSelection) return; // 🔥 חוסם חיפוש בזמן בחירה ידנית

                String q = (s == null) ? "" : s.toString().trim();
                scheduleSymbolSearch(q);
            }
        });
    }

    private void setupClickListeners() {
        if (btnLoad != null) {
            btnLoad.setOnClickListener(v -> {
                String userInput = (tickerInput == null) ? "" : tickerInput.getText().toString().trim();
                if (!userInput.isEmpty()) {
                    openChartFromInput(userInput);
                }
                hideKeyboard();
                if (tickerInput != null) tickerInput.clearFocus();
            });
        }

        if (btnChartRefresh != null) {
            btnChartRefresh.setOnClickListener(v -> {
                fetchStockData(symbol, interval);
                Toast.makeText(requireContext(), "Chart refreshed", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnTimeFrame != null) {
            btnTimeFrame.setOnClickListener(v -> {
                TimeFrameFragment dialog = new TimeFrameFragment();
                dialog.show(getChildFragmentManager(), "timeframe");
            });
        }

        if (btnToggleChart != null) {
            btnToggleChart.setOnClickListener(v -> {
                isCandleStick = !isCandleStick;
                if (isCandleStick) {
                    btnToggleChart.setText("Line chart");
                    candleStickChart.setVisibility(View.VISIBLE);
                    lineChart.setVisibility(View.GONE);
                } else {
                    btnToggleChart.setText("Candle chart");
                    candleStickChart.setVisibility(View.GONE);
                    lineChart.setVisibility(View.VISIBLE);
                }
                fetchStockData(symbol, interval);
            });
        }

        if (btnAIAnalysis != null) {
            btnAIAnalysis.setOnClickListener(v -> analyzeWithAI());
        }
    }

    private void openChartFromInput(String userInput) {
        String q = userInput.trim();
        if (q.isEmpty()) return;

        boolean looksLikeTicker = q.matches("^[A-Za-z0-9./-]{1,20}$") && !q.contains(" ");
        if (looksLikeTicker) {
            setSymbolAndLoad(q.toUpperCase(Locale.US));
            return;
        }

        // אם זה שם חברה/טקסט חופשי, ננסה לפתוח את ההתאמה הראשונה
        resolveFirstMatchAndOpen(q);
    }

    private void setSymbolAndLoad(String sym) {
        symbol = sym;

        if (tickerText != null) tickerText.setText("Ticker: " + symbol);
        if (getActivity() != null) getActivity().setTitle("Chart: " + symbol);

        fetchStockData(symbol, interval);
        hideKeyboard();
    }

    private void resolveFirstMatchAndOpen(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String url = "https://api.twelvedata.com/symbol_search?symbol=" + encoded +
                    "&outputsize=1&apikey=" + API_KEY;

            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) { }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) return;

                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray data = json.optJSONArray("data");
                        if (data == null || data.length() == 0) return;

                        JSONObject first = data.optJSONObject(0);
                        if (first == null) return;

                        String sym = first.optString("symbol", "").trim();
                        if (sym.isEmpty()) return;

                        String finalSym = sym.toUpperCase(Locale.US);

                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (tickerInput != null) {
                                tickerInput.setText(finalSym);
                                tickerInput.setSelection(finalSym.length());
                            }
                            setSymbolAndLoad(finalSym);
                        });

                    } catch (Exception ignored) { }
                }
            });

        } catch (Exception ignored) { }
    }

    private void scheduleSymbolSearch(String q) {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        pendingSearch = null;

        if (symbolSearchCall != null) {
            symbolSearchCall.cancel();
            symbolSearchCall = null;
        }

        if (q == null) q = "";
        q = q.trim();
        latestQuery = q;

        // אם אין טקסט – ננקה ונצא
        if (q.length() < 1) {
            clearSuggestions();
            return;
        }

        // הגנה פשוטה כדי לא לחפור על טקסט ארוך מדי
        if (q.length() > 50) {
            clearSuggestions();
            return;
        }

        final String finalQ = q;
        pendingSearch = () -> fetchSymbolSuggestions(finalQ);
        searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void clearSuggestions() {
        if (suggestionAdapter != null) {
            suggestionAdapter.clear();
            suggestionAdapter.notifyDataSetChanged();
        }
        if (tickerInput != null) tickerInput.dismissDropDown();
    }

    private void fetchSymbolSuggestions(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String url = "https://api.twelvedata.com/symbol_search?symbol=" + encoded +
                    "&outputsize=20&apikey=" + API_KEY;

            Request request = new Request.Builder().url(url).build();
            symbolSearchCall = client.newCall(request);

            symbolSearchCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) { }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) return;

                    ArrayList<StockSuggestion> list = new ArrayList<>();

                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray data = json.optJSONArray("data");
                        if (data == null) return;

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject o = data.optJSONObject(i);
                            if (o == null) continue;

                            String sym = o.optString("symbol", "").trim();
                            if (sym.isEmpty()) continue;

                            String name = o.optString("instrument_name", "");
                            String ex = o.optString("exchange", "");

                            list.add(new StockSuggestion(sym, name, ex));
                        }
                    } catch (Exception ignored) { }

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if (!query.equals(latestQuery)) return;  // מונע race של תוצאות ישנות
                        if (suggestionAdapter == null) return;

                        suggestionAdapter.clear();
                        suggestionAdapter.addAll(list);
                        suggestionAdapter.notifyDataSetChanged();

                        if (tickerInput != null && tickerInput.hasFocus() && !list.isEmpty()) {
                            tickerInput.showDropDown();
                        } else if (tickerInput != null) {
                            tickerInput.dismissDropDown();
                        }
                    });
                }
            });

        } catch (Exception ignored) { }
    }

    @Override
    public void onTimeFrameSelected(String interval) {
        this.interval = interval;
        if (timeFrameText != null) timeFrameText.setText("Time frame: " + interval);
        fetchStockData(symbol, interval);
    }

    private void analyzeWithAI() {
        if (fullCloses.isEmpty() || fullCloses.size() < 2) {
            Toast.makeText(requireContext(), "טען נתוני גרף קודם (מינימום 2 נקודות)", Toast.LENGTH_SHORT).show();
            return;
        }
        showCustomAIDialog();
    }

    private void showCustomAIDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ai_chat, null);

        TextView tvHint = dialogView.findViewById(R.id.tv_hint);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_ai);
        TextView tvResponse = dialogView.findViewById(R.id.tv_response);
        Button btnSend = dialogView.findViewById(R.id.btn_send);
        android.widget.EditText etQuestion = dialogView.findViewById(R.id.et_question);

        tvHint.setText("דוגמאות: 'מה דעתך על השקעה קצרת טווח?' או 'האם לקנות עכשיו?'");

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle("🤖 שאל את ה-AI")
                .setNegativeButton("ביטול", null)
                .create();

        btnSend.setOnClickListener(v -> {
            String question = etQuestion.getText().toString().trim();
            if (question.isEmpty()) {
                Toast.makeText(requireContext(), "הקלד שאלה", Toast.LENGTH_SHORT).show();
                return;
            }
            sendQuestionToAI(question, tvResponse, progressBar, etQuestion, dialog);
        });

        dialog.show();
        etQuestion.requestFocus();
    }

    private void sendQuestionToAI(String question, TextView tvResponse,
                                  ProgressBar progressBar, android.widget.EditText etQuestion, AlertDialog dialog) {
        progressBar.setVisibility(View.VISIBLE);
        etQuestion.setEnabled(false);

        String context = String.format(
                Locale.US,
                "מניה: %s | מחיר נוכחי: $%.2f | טווח זמן: %s | %d נקודות נתונים",
                symbol, lastPrice, interval, fullCloses.size()
        );

        llmService.askQuestion(symbol, question, context, fullCloses, new LLMService.AnalysisCallback() {
            @Override
            public void onAnalysisReceived(String analysis) {
                progressBar.setVisibility(View.GONE);
                etQuestion.setEnabled(true);
                etQuestion.setText("");
                tvResponse.setText(analysis);
                tvResponse.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                etQuestion.setEnabled(true);
                tvResponse.setText("❌ שגיאה: " + error);
            }
        });
    }

    private void showAnalysisDialog(String analysis) {
        String fullMessage = "🚨 <b><big><font color='#FF0000'>מחיר נוכחי: $" +
                df.format(lastPrice) + "</font></big></b> 🚨<br><br>" +
                "<b>סימול:</b> " + symbol + "<br><br>" + analysis;

        new AlertDialog.Builder(requireContext())
                .setTitle("🤖 ניתוח AI - Perplexity")
                .setMessage(Html.fromHtml(fullMessage))
                .setPositiveButton("סגור", null)
                .setNeutralButton("שמור היסטוריה", (dialog, which) -> saveAnalysis(symbol, analysis))
                .show();
    }

    private void saveAnalysis(String symbol, String analysis) {
        try {
            DatabaseReference analysesRef = FirebaseDatabase.getInstance()
                    .getReference("ai-analyses").child(symbol);
            String key = analysesRef.push().getKey();
            HashMap<String, Object> data = new HashMap<>();
            data.put("timestamp", System.currentTimeMillis());
            data.put("analysis", analysis);
            data.put("symbol", symbol);
            data.put("price", lastPrice);
            analysesRef.child(key).setValue(data);
            Toast.makeText(requireContext(), "נשמר בהיסטוריה ✅", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "שגיאת שמירה", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        if (getActivity() == null) return;

        View view = getActivity().getCurrentFocus();
        if (view == null) view = getView();

        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void fetchStockData(String symbol, String interval) {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol +
                "&interval=" + interval + "&apikey=" + API_KEY + "&outputsize=40";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray series = json.getJSONArray("values");

                    fullCloses.clear();
                    List<CandleEntry> candleEntries = new ArrayList<>();
                    float lastClose = 0, prevClose = 0;

                    if (series.length() > 0) {
                        lastClose = Float.parseFloat(series.getJSONObject(0).getString("close"));
                        lastPrice = lastClose;
                    }
                    if (series.length() > 1) {
                        prevClose = Float.parseFloat(series.getJSONObject(1).getString("close"));
                    }

                    for (int i = 0; i < series.length(); i++) {
                        JSONObject data = series.getJSONObject(i);
                        float close = Float.parseFloat(data.getString("close"));
                        fullCloses.add(close);
                    }

                    if (fullCloses.isEmpty()) return;

                    int chartIndex = 0;
                    int startIndex = Math.max(0, series.length() - 40);
                    for (int i = series.length() - 1; i >= startIndex; i--) {
                        JSONObject data = series.getJSONObject(i);
                        float open = Float.parseFloat(data.getString("open"));
                        float high = Float.parseFloat(data.getString("high"));
                        float low = Float.parseFloat(data.getString("low"));
                        float close = Float.parseFloat(data.getString("close"));
                        candleEntries.add(new CandleEntry(chartIndex++, high, low, open, close));
                    }

                    currentEntries.clear();
                    currentEntries.addAll(candleEntries);

                    float change = lastClose - prevClose;
                    float changePercent = (prevClose != 0) ? (change / prevClose) * 100 : 0;

                    final float dispClose = lastClose;
                    final float dispChange = change;
                    final float dispChangePercent = changePercent;
                    final String currentSymbol = symbol;
                    final List<CandleEntry> finalEntries = new ArrayList<>(candleEntries);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isCandleStick) updateCandleChart(finalEntries);
                            else updateLineChart(finalEntries);

                            if (priceText != null) priceText.setText("Current price: $" + df.format(dispClose));
                            if (changeText != null) {
                                changeText.setText("Daily change: $" + String.format(Locale.US, "%.2f", dispChange) +
                                        " (" + String.format(Locale.US, "%.2f", dispChangePercent) + "%)");
                            }
                            if (timeFrameText != null) timeFrameText.setText("Time frame: " + interval);
                            if (tickerText != null) tickerText.setText("Ticker: " + currentSymbol);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }



    private void updateCandleChart(List<CandleEntry> entries) {
        CandleDataSet dataSet = new CandleDataSet(entries, "Stock candle chart");
        dataSet.setDecreasingColor(Color.RED);
        dataSet.setIncreasingColor(Color.GREEN);
        dataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        dataSet.setShadowColor(Color.DKGRAY);
        dataSet.setDrawValues(false);

        XAxis xAxis = candleStickChart.getXAxis();
        YAxis leftAxis = candleStickChart.getAxisLeft();
        YAxis rightAxis = candleStickChart.getAxisRight();
        xAxis.setDrawGridLines(false);
        leftAxis.setDrawGridLines(false);
        rightAxis.setDrawGridLines(false);

        CandleData data = new CandleData(dataSet);
        candleStickChart.setData(data);
        candleStickChart.invalidate();
    }

    private void updateLineChart(List<CandleEntry> candleEntries) {
        List<Entry> lineEntries = new ArrayList<>();
        for (CandleEntry c : candleEntries) {
            lineEntries.add(new Entry(c.getX(), c.getClose()));
        }
        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Line chart");
        lineDataSet.setColor(Color.BLUE);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    public static ChartFragment newInstance(String symbol) {
        ChartFragment fragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putString("symbol", symbol);
        fragment.setArguments(args);
        return fragment;
    }


}
