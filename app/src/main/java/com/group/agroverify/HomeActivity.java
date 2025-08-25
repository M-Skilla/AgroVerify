package com.group.agroverify;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.group.agroverify.utils.LocaleHelper;

public class HomeActivity extends AppCompatActivity {

    private ShapeableImageView languageBtn;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

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

        initializeViews();
        setupClickListeners();
        updateLanguageButton();
    }

    private void initializeViews() {
        languageBtn = findViewById(R.id.languageBtn);
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

        // Language Switch Button
        languageBtn.setOnClickListener(v -> {
            switchLanguage();
        });
    }

    private void switchLanguage() {
        LocaleHelper.switchLanguage(this);
        updateLanguageButton();
        recreateActivity();
    }

    private void updateLanguageButton() {
        if (LocaleHelper.isSwahili(this)) {
            // Currently Swahili, show Tanzania flag
            languageBtn.setImageResource(R.drawable.tanzania_flag);
        } else {
            // Currently English, show UK flag
            languageBtn.setImageResource(R.drawable.uk_flag);
        }
    }

    private void recreateActivity() {
        // Recreate the activity to apply the new locale
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}