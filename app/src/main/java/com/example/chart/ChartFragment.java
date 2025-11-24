package com.example.chart;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChartFragment extends Fragment implements TimeFrameFragment.TimeFrameListener {

    private CandleStickChart candleStickChart;
    private LineChart lineChart;
    private EditText tickerInput;
    private Button btnLoad, btnTimeFrame, btnToggleChart;
    private ImageButton btnChartRefresh;
    private TextView priceText, changeText, timeFrameText, tickerText;
    private final OkHttpClient client = new OkHttpClient();
    private final String API_KEY = "0518811f0d394fa39842a8024a25c049";
    private String symbol = "SPY";
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
        btnChartRefresh = v.findViewById(R.id.btnChartRefresh);
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

        // מאזין ללחיצה על כפתור טעינת הסימבול
        btnLoad.setOnClickListener(v1 -> {
            String userInput = tickerInput.getText().toString().trim();
            if (!userInput.isEmpty()) {
                symbol = userInput.toUpperCase();
                tickerText.setText("Ticker: " + symbol);
                if (getActivity() != null) {
                    getActivity().setTitle("Chart: " + symbol);
                }
                fetchStockData(symbol, interval);
            }
            hideKeyboard();
            tickerInput.clearFocus();
        });

        // מאזין ללחיצה על כפתור הריפרש - מוגדר בנפרד ומוכן לפעולה תמידית
        btnChartRefresh.setOnClickListener(v2 -> {
            fetchStockData(symbol, interval);
            Toast.makeText(requireContext(), "Watchlist refreshed", Toast.LENGTH_SHORT).show();
        });

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

//    private void updateChart(List<CandleEntry> entries) {
//        CandleDataSet dataSet = new CandleDataSet(entries, "Price");
//        dataSet.setColor(Color.rgb(80, 80, 80));
//        dataSet.setShadowColor(Color.DKGRAY);
//        dataSet.setShadowWidth(0.8f);
//        dataSet.setDecreasingColor(Color.RED);
//        dataSet.setDecreasingPaintStyle(Paint.Style.FILL);
//        dataSet.setIncreasingColor(Color.GREEN);
//        dataSet.setIncreasingPaintStyle(Paint.Style.FILL);
//        dataSet.setNeutralColor(Color.BLUE); // צבע לנרות בלי שינוי
//        dataSet.setDrawValues(false); // לא להציג ערכים על גרף
//
//        CandleData candleData = new CandleData(dataSet);
//        candleStickChart.setData(candleData);
//
//        candleStickChart.getDescription().setEnabled(false);
//        candleStickChart.setPinchZoom(true);
//        candleStickChart.setDrawGridBackground(false);
//        candleStickChart.setHighlightPerDragEnabled(true);
//
//        XAxis xAxis = candleStickChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setDrawGridLines(false);
//
//        YAxis leftAxis = candleStickChart.getAxisLeft();
//        leftAxis.setDrawGridLines(true);
//        candleStickChart.getAxisRight().setEnabled(false);
//
//        candleStickChart.invalidate(); // מצייר את הגרף מחדש
//    }

    private void updateChart(List<CandleEntry> entries) {
        CandleDataSet dataSet = new CandleDataSet(entries, "Stock candle chart");
        dataSet.setDecreasingColor(Color.RED);
        dataSet.setIncreasingColor(Color.GREEN);

        // מילוי מלא לנרות עולים ויורדים
        dataSet.setIncreasingPaintStyle(Paint.Style.FILL);

        dataSet.setShadowColor(Color.DKGRAY);
        dataSet.setDrawValues(false);

        CandleData data = new CandleData(dataSet);
        candleStickChart.setData(data);
        candleStickChart.invalidate(); // מצייר את הגרף מחדש
    }





        private void updateLineChart(List<CandleEntry> candleEntries) {
        List<Entry> lineEntries = new ArrayList<>();
        for (CandleEntry c : candleEntries) {
            lineEntries.add(new Entry(c.getX(), c.getClose()));
        }

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Line chart");
        lineDataSet.setColor(Color.BLUE);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // מצייר את הגרף מחדש
    }

}
