package com.group.agroverify;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupClickListeners();
    }

    private void setupClickListeners() {
        // QR Code Scan Button
        MaterialCardView scanQrCard = findViewById(R.id.scanQrCard);
        scanQrCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, QrScanActivity.class);
            startActivity(intent);
        });

        // Verification History Card
        MaterialCardView verificationHistoryCard = findViewById(R.id.verificationHistoryCard);
        if (verificationHistoryCard != null) {
            verificationHistoryCard.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, VerificationHistoryActivity.class);
                startActivity(intent);
            });
        }
    }
}