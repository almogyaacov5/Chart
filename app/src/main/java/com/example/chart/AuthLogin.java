package com.example.chart;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executor;

public class AuthLogin extends AppCompatActivity {

    private EditText editTextEmailAddress, editTextPassword;
    private Button button;
    private Button btnBiometricLogin;
    private FirebaseAuth refAuth;

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onStart() {
        super.onStart();
        // בדיקה אוטומטית אם המשתמש כבר מחובר
        // אם כן, מדלגים ישר ל-MainActivity בלי לבקש התחברות
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(AuthLogin.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_login); // layout של ה-Login

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextEmailAddress = findViewById(R.id.editTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextPassword);
        button = findViewById(R.id.button);
        btnBiometricLogin = findViewById(R.id.btnBiometricLogin); // תוודא שקיים ב-XML

        refAuth = FirebaseAuth.getInstance();

        button.setOnClickListener(v -> loginUser());

        // הגדרת BiometricPrompt
        setupBiometricPrompt();

        // כפתור כניסה עם טביעת אצבע
        btnBiometricLogin.setOnClickListener(v -> {
            BiometricManager manager = BiometricManager.from(this);
            int canAuth = manager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                Toast.makeText(this,
                        "המכשיר לא תומך או שלא מוגדרת טביעת אצבע",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser() {
        String email = editTextEmailAddress.getText().toString().trim();
        String pass = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Logging in ...");
        pd.setCancelable(false);
        pd.show();

        refAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            Log.i("AuthLogin", "signInWithEmailAndPassword: success");

                            // שמירת פרטי התחברות לשימוש עתידי ב-Biometric (לשימוש לימודי)
                            SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("email", email)
                                    .putString("password", pass)
                                    .apply();

                            Toast.makeText(getApplicationContext(),
                                    "User logged in successfully",
                                    Toast.LENGTH_SHORT).show();

                            // מעבר לאפליקציית ההשקעות
                            Intent intent = new Intent(AuthLogin.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e("AuthLogin", "signInWithEmailAndPassword: failure", task.getException());
                            Toast.makeText(getApplicationContext(),
                                    "Authentication failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // הגדרת BiometricPrompt
    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // אם טביעת האצבע הצליחה – להתחבר ל-Firebase עם הפרטים השמורים
                        loginWithSavedCredentials();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(getApplicationContext(),
                                "Biometric error: " + errString,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(getApplicationContext(),
                                "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("כניסה עם טביעת אצבע")
                .setSubtitle("אשר זהות כדי להיכנס לחשבון ההשקעות")
                .setNegativeButtonText("ביטול")
                .build();
    }

    // התחברות אוטומטית ל-Firebase לפי האימייל+סיסמה השמורים
    private void loginWithSavedCredentials() {
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        String savedEmail = prefs.getString("email", null);
        String savedPass = prefs.getString("password", null);

        if (savedEmail == null || savedPass == null) {
            Toast.makeText(this,
                    "אין פרטי התחברות שמורים, התחבר פעם אחת עם אימייל+סיסמה",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Logging in ...");
        pd.setCancelable(false);
        pd.show();

        refAuth.signInWithEmailAndPassword(savedEmail, savedPass)
                .addOnCompleteListener(this, task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(),
                                "Logged in with biometrics",
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(AuthLogin.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Firebase login failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
