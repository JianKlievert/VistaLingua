package com.jethers.mobcompfinalproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailText, passwordText;
    private FirebaseAuth auth; // Firebase Authentication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize Views
        emailText = findViewById(R.id.EmailText);
        passwordText = findViewById(R.id.PasswordText);
        Button loginButton = findViewById(R.id.LoginBtn);
        TextView signUpText = findViewById(R.id.SignUpText);
        TextView forgotPasswordText = findViewById(R.id.ForgotPassword);

        // Set up listeners
        loginButton.setOnClickListener(view -> handleLogin());
        signUpText.setOnClickListener(view -> navigateToSignUp());
        forgotPasswordText.setOnClickListener(view -> handleForgotPassword());
    }

    private void handleLogin() {
        String email = emailText.getText().toString().trim();
        String password = passwordText.getText().toString().trim();

        if (validateInput(email, password)) {
            loginWithFirebase(email, password);
        }
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            emailText.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordText.setError("Password is required");
            return false;
        }
        return true;
    }

    private void loginWithFirebase(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, MenuActivity.class); // Replace with your Home Activity
        startActivity(intent);
        finish();
    }

    private void handleForgotPassword() {
        String email = emailText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailText.setError("Enter your registered email");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
