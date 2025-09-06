package com.group.agroverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;

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
    private TextView welcomeText;
    private SharedPreferences sharedPreferences;
    private String distributorId;
    private String distributorName = "";

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
        loadDistributorInfo();
        setupClickListeners();
        updateLanguageButton();
        updateWelcomeText();
    }

    private void initializeViews() {
        languageBtn = findViewById(R.id.languageBtn);
        welcomeText = findViewById(R.id.welcomeText); // This may need to be added to layout
    }

    private void loadDistributorInfo() {
        sharedPreferences = getSharedPreferences("DistributorPrefs", MODE_PRIVATE);

        // Get distributor info from intent (new login) or SharedPreferences (existing session)
        Intent intent = getIntent();
        if (intent.hasExtra("distributorId")) {
            distributorId = intent.getStringExtra("distributorId");
            distributorName = intent.getStringExtra("distributorName");
        } else {
            distributorId = sharedPreferences.getString("distributorId","");
            distributorName = sharedPreferences.getString("distributorName","");
        }
    }

    private void updateWelcomeText() {
        if (welcomeText != null && !distributorName.isEmpty()) {
            welcomeText.setText("Welcome, " + distributorName);
        }
    }

    private void setupClickListeners() {
        // QR Code Scan Button
        MaterialCardView scanQrCard = findViewById(R.id.scanQrCard);
        scanQrCard.setOnClickListener(v -> {
            Intent intent;
                // Use distributor-specific QR scan activity
                intent = new Intent(HomeActivity.this, QrScanActivity.class);
                intent.putExtra("distributorId", distributorId);
                intent.putExtra("distributorName", distributorName);
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