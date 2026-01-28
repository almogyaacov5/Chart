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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRegister extends AppCompatActivity {

    private EditText editTextEmailAddress, editTextPassword;
    private Button button, goToLogIn;
    private FirebaseAuth refAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_register); // layout של ה-Register

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextEmailAddress = findViewById(R.id.editTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextPassword);
        button = findViewById(R.id.button);
        goToLogIn = findViewById(R.id.GoToLogIn);

        refAuth = FirebaseAuth.getInstance();

        // כפתור יצירת משתמש
        button.setOnClickListener(v -> createUser());

        // מעבר למסך לוגין
        goToLogIn.setOnClickListener(v -> {
            Intent intent = new Intent(AuthRegister.this, AuthLogin.class);
            startActivity(intent);
        });
    }

    private void createUser() {
        String email = editTextEmailAddress.getText().toString().trim();
        String pass = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Creating user ...");
        pd.setCancelable(false);
        pd.show();

        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            Log.i("AuthRegister", "createUserWithEmailAndPassword: success");
                            FirebaseUser user = refAuth.getCurrentUser();
                            Toast.makeText(getApplicationContext(),
                                    "User created successfully",
                                    Toast.LENGTH_SHORT).show();

                            // שמירת פרטי התחברות לשימוש עתידי ב-Biometric (לשימוש לימודי, לא לפרודקשן)
                            SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("email", email)
                                    .putString("password", pass)
                                    .apply();

                            // מעבר לאפליקציית ההשקעות
                            Intent intent = new Intent(AuthRegister.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e("AuthRegister", "createUserWithEmailAndPassword: failure", task.getException());
                            Toast.makeText(getApplicationContext(),
                                    "Failed to create user",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
