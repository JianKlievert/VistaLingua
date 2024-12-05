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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText emailText, passwordText, confPasswordText;
    private FirebaseAuth auth; // Firebase Authentication
    private DatabaseReference databaseReference; // Firebase Realtime Database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Get references to the views
        emailText = findViewById(R.id.EmailText);
        passwordText = findViewById(R.id.PasswordText);
        confPasswordText = findViewById(R.id.ConfPasswordText);
        TextView loginText = findViewById(R.id.LoginText); // Move initialization here
        Button signUpButton = findViewById(R.id.SignUpBtn);

        // Attach a click listener to the Sign-Up button
        signUpButton.setOnClickListener(view -> handleSignUp());
        loginText.setOnClickListener(view -> navigateToLogin());
    }



    private void handleSignUp() {
        String email = emailText.getText().toString().trim();
        String password = passwordText.getText().toString().trim();
        String confirmPassword = confPasswordText.getText().toString().trim();

        if (validateInput(email, password, confirmPassword)) {
            signUpWithFirebaseAuth(email, password);
        }
    }


    private boolean validateInput(String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email)) {
            emailText.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordText.setError("Password is required");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confPasswordText.setError("Passwords do not match");
            return false;
        }
        if (password.length() < 6) {
            passwordText.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }


    private void signUpWithFirebaseAuth(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign-up success: Save user info to Realtime Database
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveToFirebase(firebaseUser.getUid(), email);
                        }
                    } else {
                        // Handle errors
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(SignUpActivity.this, "This email is already registered", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Sign-up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void saveToFirebase(String userId, String email) {
        User user = new User(email);

        databaseReference.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                        // Navigate to LoginActivity
                        navigateToLogin();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Failed to save user info: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to navigate to LoginActivity
    private void navigateToLogin() {
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close SignUpActivity to prevent returning to it
    }


    public static class User {
        public String email;

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public User(String email) {
            this.email = email;
        }
    }
}
