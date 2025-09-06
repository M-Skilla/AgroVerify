package com.group.agroverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.group.agroverify.dtos.DistributorLoginRequest;
import com.group.agroverify.dtos.DistributorLoginResponse;
import com.group.agroverify.service.DistributorService;
import com.group.agroverify.utils.ApiClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DistributorLoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private MaterialCardView loadingOverlay;
    private DistributorService distributorService;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        initializeServices();

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToHome();
            return;
        }

        setupClickListeners();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.ninEditText);
        etPassword = findViewById(R.id.passwordEditText);
        btnLogin = findViewById(R.id.loginButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void initializeServices() {
        distributorService = ApiClient.getRetrofit().create(DistributorService.class);
        sharedPreferences = getSharedPreferences("DistributorPrefs", MODE_PRIVATE);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Show loading
        showLoading(true);

        // Create login request
        DistributorLoginRequest loginRequest = new DistributorLoginRequest(username, password);

        // Make API call
        Call<DistributorLoginResponse> call = distributorService.login(loginRequest);
        call.enqueue(new Callback<DistributorLoginResponse>() {
            @Override
            public void onResponse(Call<DistributorLoginResponse> call, Response<DistributorLoginResponse> response) {
                showLoading(false);
                try (ResponseBody errorBody = response.errorBody()) {
                    if (errorBody != null) {
                        String errorMessage = errorBody.string();
                        Toast.makeText(DistributorLoginActivity.this,
                                "Login failed: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (response.isSuccessful() && response.body() != null) {
                    DistributorLoginResponse loginResponse = response.body();




                    if (loginResponse.isSuccess() && loginResponse.getDistributor() != null) {
                        // Save distributor info to SharedPreferences
                        saveDistributorInfo(loginResponse.getDistributor().getId(),
                                          loginResponse.getDistributor().getName(),
                                          loginResponse.getDistributor().getUsername());

                        // Navigate to HomeActivity
                        Intent intent = new Intent(DistributorLoginActivity.this, HomeActivity.class);
                        intent.putExtra("distributorId", loginResponse.getDistributor().getId());
                        intent.putExtra("distributorName", loginResponse.getDistributor().getName());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(DistributorLoginActivity.this,
                                     loginResponse.getMessage() != null ? loginResponse.getMessage() : "Login failed",
                                     Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(DistributorLoginActivity.this,
                                 "Login failed. Please check your credentials.",
                                 Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<DistributorLoginResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(DistributorLoginActivity.this,
                             "Network error: " + t.getMessage(),
                             Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveDistributorInfo(String distributorId, String distributorName, String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("distributorId", distributorId);
        editor.putString("distributorName", distributorName);
        editor.putString("distributorUsername", username);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    private void navigateToHome() {
        Intent intent = new Intent(DistributorLoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
