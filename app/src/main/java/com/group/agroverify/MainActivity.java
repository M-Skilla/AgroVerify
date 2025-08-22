package com.group.agroverify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.group.agroverify.fragment.NoInternetDialog;
import com.group.agroverify.utils.KeyStorage;
import com.group.agroverify.viewmodels.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getKeyStore().observe(this, keyStore -> {
            if (keyStore != null) {
                KeyStorage.setPublicKey(MainActivity.this, keyStore.getPublicKey());
                moveToHome();
            } else {
                if (KeyStorage.getPublicKey(MainActivity.this) != null) {
                    moveToHome();
                } else {
//                    new NoInternetDialog().show(getSupportFragmentManager(), "NO_INTERNET");
                        Toast.makeText(this, "Check your internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.fetchPublicKey();
    }

    public void moveToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}