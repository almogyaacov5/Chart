package com.example.chart;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class ChartFragment extends Fragment implements TimeFrameFragment.TimeFrameListener {

    private CandleStickChart candleStickChart;
    private LineChart lineChart;
    private EditText tickerInput;
    private Button btnLoad, btnTimeFrame, btnToggleChart;
    private TextView priceText, changeText, timeFrameText, tickerText;
    private final OkHttpClient client = new OkHttpClient();
    private final String API_KEY = "0518811f0d394fa39842a8024a25c049";
    private String symbol = "UBER";
    private String interval = "1day";
    private boolean isCandleStick = true;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chart, container, false);

        candleStickChart = v.findViewById(R.id.stock_chart);
        lineChart = v.findViewById(R.id.line_chart);
        tickerInput = v.findViewById(R.id.ticker_input);
        btnLoad = v.findViewById(R.id.btnLoad);
        btnTimeFrame = v.findViewById(R.id.btnSelectTimeFrame);
        btnToggleChart = v.findViewById(R.id.btnToggleChart);
        priceText = v.findViewById(R.id.priceText);
        changeText = v.findViewById(R.id.changeText);
        timeFrameText = v.findViewById(R.id.timeFrameText);
        tickerText = v.findViewById(R.id.tickerText);

        // קבלת הסימבול מה-arguments
        if (getArguments() != null && getArguments().containsKey("symbol")) {
            symbol = getArguments().getString("symbol", symbol);
            if (tickerInput != null) tickerInput.setText(symbol);
        }

        // עדכון כותרת לפי המניה:
        if (getActivity() != null) {
            getActivity().setTitle("Chart: " + symbol);
        }

        timeFrameText.setText("Time frame: " + interval);
        tickerText.setText("Ticker: " + symbol);

        candleStickChart.setVisibility(View.VISIBLE);
        lineChart.setVisibility(View.GONE);

        btnLoad.setOnClickListener(v1 -> {
            String userInput = tickerInput.getText().toString().trim();
            ImageButton btnChartRefresh = v1.getRootView().findViewById(R.id.btnChartRefresh);
            if (!userInput.isEmpty()) {
                String symbol = userInput.toUpperCase();
                tickerText.setText("Ticker: " + symbol);
                if (getActivity() != null) {
                    getActivity().setTitle("Chart: " + symbol);
                }
                fetchStockData(symbol, interval);

                // כאן מגדירים את כפתור הריפרש עם פעולה נכונה
                btnChartRefresh.setOnClickListener(v2 -> {
                    fetchStockData(symbol, interval);
                });
            }
            hideKeyboard();
            tickerInput.clearFocus();

        });


        // כאן הוספת פתיחת דיאלוג טיים פריים:
        btnTimeFrame.setOnClickListener(v1 -> {
            TimeFrameFragment dialog = new TimeFrameFragment();
            dialog.show(getChildFragmentManager(), "timeframe");
        });

        btnToggleChart.setOnClickListener(v1 -> {
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

        fetchStockData(symbol, interval);
        return v;
    }

    @Override
    public void onTimeFrameSelected(String interval) {
        this.interval = interval;
        timeFrameText.setText("Time frame: " + interval);
        fetchStockData(symbol, interval);
    }


    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view == null) view = getView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void fetchStockData(String symbol, String interval) {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol +
                "&interval=" + interval + "&apikey=" + API_KEY + "&outputsize=40";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray series = json.getJSONArray("values");
                    List<CandleEntry> candleEntries = new ArrayList<>();
                    float lastClose = 0, prevClose = 0;
                    if (series.length() > 0) lastClose = Float.parseFloat(series.getJSONObject(0).getString("close"));
                    if (series.length() > 1) prevClose = Float.parseFloat(series.getJSONObject(1).getString("close"));
                    int chartIndex = 0;
                    for (int i = series.length() - 1; i >= 0 && chartIndex < 40; i--, chartIndex++) {
                        JSONObject data = series.getJSONObject(i);
                        float open = Float.parseFloat(data.getString("open"));
                        float high = Float.parseFloat(data.getString("high"));
                        float low = Float.parseFloat(data.getString("low"));
                        float close = Float.parseFloat(data.getString("close"));
                        candleEntries.add(new CandleEntry(chartIndex, high, low, open, close));
                    }
                    float change = lastClose - prevClose;
                    float changePercent = (prevClose != 0) ? (change / prevClose) * 100 : 0;
                    final float dispClose = lastClose;
                    final float dispChange = change;
                    final float dispChangePercent = changePercent;
                    final String currentSymbol = symbol;

                    if(getActivity()!=null) {
                        getActivity().runOnUiThread(() -> {
                            if (isCandleStick) {
                                updateChart(candleEntries);
                            } else {
                                updateLineChart(candleEntries);
                            }
                            priceText.setText("Current price: " + dispClose);
                            changeText.setText("Daily change: " + String.format("%.2f", dispChange) +
                                    " (" + String.format("%.2f", dispChangePercent) + "%)");
                            timeFrameText.setText("Time frame: " + interval);
                            tickerText.setText("Ticker: " + currentSymbol);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateChart(List<CandleEntry> entries) {
        CandleDataSet dataSet = new CandleDataSet(entries, "Stock chart");
        dataSet.setDecreasingColor(android.graphics.Color.RED);
        dataSet.setIncreasingColor(android.graphics.Color.GREEN);
        dataSet.setShadowColor(android.graphics.Color.DKGRAY);
        dataSet.setIncreasingPaintStyle(android.graphics.Paint.Style.FILL);
        dataSet.setDrawValues(false);

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
        lineDataSet.setColor(android.graphics.Color.BLUE);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }
}
