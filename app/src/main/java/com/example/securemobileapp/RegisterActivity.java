package com.example.securemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.securemobileapp.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    ActivityRegisterBinding binding;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    AuthenticationModule authenticationModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        authenticationModule = new AuthenticationModule();

        if(firebaseAuth.getCurrentUser() != null){
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        }
        binding.registerButton.setOnClickListener(v -> {
            String emailValue = binding.email.getText().toString().trim();
            String passwordValue = binding.password.getText().toString().trim();
            String confirm_pwdValue = binding.confirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(emailValue) || TextUtils.isEmpty(passwordValue) || TextUtils.isEmpty(confirm_pwdValue)) {
                Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            authenticationModule.register(emailValue, passwordValue)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        assert user != null;
                        Toast.makeText(RegisterActivity.this, "Registered as " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        // Перехід на активність входу або головну
                        DocumentReference df = firebaseFirestore.collection("Users").document(user.getUid());
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("Email", emailValue);
                        df.set(userInfo);
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }).addOnFailureListener(e -> {
                        // Помилка при реєстрації
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}