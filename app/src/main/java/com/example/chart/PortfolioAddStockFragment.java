package com.example.chart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PortfolioAddStockFragment extends Fragment {

    private EditText editTicker, editBuyPrice;
    private Button btnAddStock;
    private DatabaseReference stocksRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.portfolio_add_stock_fragment, container, false);

        editTicker = v.findViewById(R.id.editTicker);
        editBuyPrice = v.findViewById(R.id.editBuyPrice);
        btnAddStock = v.findViewById(R.id.btnAddStock);

        // uid של המשתמש המחובר
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // נתיב תיק השקעות לפי משתמש
        stocksRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("portfolio-stocks");

        btnAddStock.setOnClickListener(view -> {
            String ticker = editTicker.getText().toString().trim().toUpperCase();
            String priceStr = editBuyPrice.getText().toString().trim();

            if (ticker.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(getContext(),
                        "Enter a ticker and price",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            float price;
            try {
                price = Float.parseFloat(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(),
                        "Invalid price",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // currentPrice == buyPrice בעת ההוספה
            StockData data = new StockData(ticker, price, price);

            // שמירה תחת users/{uid}/portfolio-stocks/{ticker}
            stocksRef.child(ticker).setValue(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(),
                                "מניה הוספה לתיק!",
                                Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().popBackStack();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(
                            getContext(),
                            "שגיאה בהוספת מניה",
                            Toast.LENGTH_SHORT
                    ).show());
        });

        return v;
    }
}