package com.group.agroverify;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.group.agroverify.adapter.VerificationHistoryAdapter;
import com.group.agroverify.database.entities.VerificationHistory;
import com.group.agroverify.service.VerificationHistoryService;
import com.group.agroverify.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class VerificationHistoryActivity extends AppCompatActivity implements VerificationHistoryAdapter.OnItemClickListener {

    private static final String TAG = "VerificationHistoryActivity";

    // UI Components
    private RecyclerView recyclerViewHistory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialButton btnBack, btnClearHistory;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipVerified, chipFailed;
    private View loadingOverlay, emptyStateContainer;
    private android.widget.TextView tvTotalScans, tvSuccessfulScans, tvFailedScans;

    // Data and Services
    private VerificationHistoryService verificationHistoryService;
    private VerificationHistoryAdapter adapter;
    private List<VerificationHistory> allHistory = new ArrayList<>();
    private List<VerificationHistory> filteredHistory = new ArrayList<>();
    private CompositeDisposable disposables = new CompositeDisposable();

    // Filter state
    private FilterType currentFilter = FilterType.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeServices();
        setupRecyclerView();
        setupClickListeners();
        setupFilters();
        loadHistoryData();
    }

    private void initializeViews() {
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnBack = findViewById(R.id.btnBack);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll = findViewById(R.id.chipAll);
        chipVerified = findViewById(R.id.chipVerified);
        chipFailed = findViewById(R.id.chipFailed);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        tvTotalScans = findViewById(R.id.tvTotalScans);
        tvSuccessfulScans = findViewById(R.id.tvSuccessfulScans);
        tvFailedScans = findViewById(R.id.tvFailedScans);
    }

    private void initializeServices() {
        verificationHistoryService = new VerificationHistoryService(this);
    }

    private void setupRecyclerView() {
        adapter = new VerificationHistoryAdapter(filteredHistory, this);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());

        swipeRefreshLayout.setOnRefreshListener(this::loadHistoryData);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) {
                currentFilter = FilterType.ALL;
            } else if (checkedId == R.id.chipVerified) {
                currentFilter = FilterType.VERIFIED;
            } else if (checkedId == R.id.chipFailed) {
                currentFilter = FilterType.FAILED;
            }

            applyFilter();
        });
    }

    private void loadHistoryData() {
        showLoading(true);

        disposables.add(
            verificationHistoryService.getAllVerificationHistory()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    history -> {
                        allHistory.clear();
                        allHistory.addAll(history);
                        applyFilter();
                        updateStatistics();
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                    },
                    throwable -> {
                        Log.e(TAG, "Error loading history", throwable);
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show();
                    }
                )
        );
    }

    private void applyFilter() {
        filteredHistory.clear();

        switch (currentFilter) {
            case ALL:
                filteredHistory.addAll(allHistory);
                break;
            case VERIFIED:
                for (VerificationHistory item : allHistory) {
                    if (item.isVerificationResult()) {
                        filteredHistory.add(item);
                    }
                }
                break;
            case FAILED:
                for (VerificationHistory item : allHistory) {
                    if (!item.isVerificationResult()) {
                        filteredHistory.add(item);
                    }
                }
                break;
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateStatistics() {
        disposables.add(
            verificationHistoryService.getVerificationStats()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    stats -> {
                        tvTotalScans.setText(String.valueOf(stats.getTotalVerifications()));
                        tvSuccessfulScans.setText(String.valueOf(stats.getSuccessfulVerifications()));
                        tvFailedScans.setText(String.valueOf(stats.getFailedVerifications()));
                    },
                    throwable -> Log.e(TAG, "Error loading statistics", throwable)
                )
        );
    }

    private void updateEmptyState() {
        if (filteredHistory.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            recyclerViewHistory.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all verification history? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearHistory())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearHistory() {
        verificationHistoryService.clearAllHistory();
        allHistory.clear();
        filteredHistory.clear();
        adapter.notifyDataSetChanged();
        updateStatistics();
        updateEmptyState();
        Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(VerificationHistory item) {
        // Handle regular item clicks if needed
        // For now, we'll just log the click
        Log.d(TAG, "Item clicked: " + item.getSerial());
    }

    @Override
    public void onReportIssueClick(VerificationHistory item) {
        showReportIssueDialog(item);
    }

    private void showReportIssueDialog(VerificationHistory item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_issue, null);

        // Initialize dialog views
        android.widget.TextView tvProductSerial = dialogView.findViewById(R.id.tvProductSerial);
        android.widget.TextView tvErrorDetails = dialogView.findViewById(R.id.tvErrorDetails);
        ChipGroup chipGroupIssueType = dialogView.findViewById(R.id.chipGroupIssueType);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        TextInputEditText etContact = dialogView.findViewById(R.id.etContact);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSubmitReport = dialogView.findViewById(R.id.btnSubmitReport);

        // Set product information
        tvProductSerial.setText("Serial: " + item.getSerial());
        tvErrorDetails.setText("Error: " + (item.getErrorMessage() != null ? item.getErrorMessage() : "Verification failed"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmitReport.setOnClickListener(v -> {
            String issueType = getSelectedIssueType(chipGroupIssueType);
            String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
            String contact = etContact.getText() != null ? etContact.getText().toString().trim() : "";

            submitIssueReport(item, issueType, description, contact);
            dialog.dismiss();
        });

        dialog.show();
    }

    private String getSelectedIssueType(ChipGroup chipGroup) {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipCounterfeit) return "Suspected Counterfeit";
        if (checkedId == R.id.chipTampered) return "Product Tampered";
        if (checkedId == R.id.chipAppError) return "App Error";
        if (checkedId == R.id.chipOther) return "Other";
        return "Suspected Counterfeit"; // Default
    }

    private void submitIssueReport(VerificationHistory item, String issueType, String description, String contact) {
        // Here you would typically send the report to your backend server
        // For now, we'll just show a success message and log the report

        Log.i(TAG, "Issue Report Submitted:");
        Log.i(TAG, "Serial: " + item.getSerial());
        Log.i(TAG, "Issue Type: " + issueType);
        Log.i(TAG, "Description: " + description);
        Log.i(TAG, "Contact: " + contact);
        Log.i(TAG, "Original Error: " + item.getErrorMessage());

        // TODO: Implement actual report submission to your backend
        // reportService.submitIssueReport(item.getSerial(), issueType, description, contact, item.getErrorMessage());

        Toast.makeText(this, "Issue report submitted successfully", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (verificationHistoryService != null) {
            verificationHistoryService.dispose();
        }
        disposables.clear();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private enum FilterType {
        ALL, VERIFIED, FAILED
    }
}
