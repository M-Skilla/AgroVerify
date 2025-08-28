package com.group.agroverify;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.group.agroverify.adapter.VerificationHistoryAdapter;
import com.group.agroverify.database.entities.VerificationHistory;
import com.group.agroverify.dtos.ReportResponse;
import com.group.agroverify.service.KeyService;
import com.group.agroverify.service.VerificationHistoryService;
import com.group.agroverify.utils.ApiClient;
import com.group.agroverify.utils.LocaleHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    // Image capture components
    private ImageView ivImagePreview;
    private MaterialButton btnTakePhoto, btnChooseGallery, btnRemoveImage;
    private Uri currentImageUri;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private AlertDialog cameraDialog;

    // Activity result launchers
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private ActivityResultLauncher<Intent> dialogGalleryLauncher;

    private ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher;

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
        setupCamera();
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
        ivImagePreview = findViewById(R.id.ivImagePreview);

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
                .setTitle(getString(R.string.clear_history))
                .setMessage(getString(R.string.clear_history_msg))
                .setPositiveButton(getString(R.string.clear), (dialog, which) -> clearHistory())
                .setNegativeButton(getString(R.string.cancel), null)
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

    private void setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor();

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            currentImageUri = imageUri;
                            Glide.with(this).load(imageUri).into(ivImagePreview);
                        }
                    }
                }
        );

        requestMultiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean cameraPermission = result.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean writeExternalPermission = result.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false);
                    Boolean readExternalPermission = result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);

                    if (cameraPermission != null && cameraPermission && writeExternalPermission != null && writeExternalPermission && readExternalPermission != null && readExternalPermission) {
                        Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        dialogGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            currentImageUri = imageUri;
                            // Update the dialog's ImageView
                            if (cameraDialog != null && cameraDialog.isShowing()) {
                                ImageView iv = cameraDialog.findViewById(R.id.ivImagePreview);
                                MaterialButton btnRemove = cameraDialog.findViewById(R.id.btnRemoveImage);
                                if (iv != null && btnRemove != null) {
                                    Glide.with(this).load(imageUri).into(iv);
                                    iv.setVisibility(View.VISIBLE);
                                    btnRemove.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                }
        );

    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder();
                imageCapture = imageCaptureBuilder.build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg");
        currentImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@androidx.annotation.NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(VerificationHistoryActivity.this, "Photo captured: " + currentImageUri, Toast.LENGTH_SHORT).show();
                        Glide.with(VerificationHistoryActivity.this).load(currentImageUri).into(ivImagePreview);
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        Toast.makeText(VerificationHistoryActivity.this, "Photo capture failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void removeImage() {
        currentImageUri = null;
        ivImagePreview.setImageDrawable(null);
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

        // Initialize image components
        ImageView ivImagePreview = dialogView.findViewById(R.id.ivImagePreview);
        MaterialButton btnTakePhoto = dialogView.findViewById(R.id.btnTakePhoto);
        MaterialButton btnChooseGallery = dialogView.findViewById(R.id.btnChooseGallery);
        MaterialButton btnRemoveImage = dialogView.findViewById(R.id.btnRemoveImage);

        // Set product information
        tvProductSerial.setText(getString(R.string.serial, item.getSerial()));
        tvErrorDetails.setText(getString(R.string.error, (item.getErrorMessage() != null ? item.getErrorMessage() : getString(R.string.verification_failed))));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_AgroVerify_FullscreenDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        // Image handling click listeners
        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                showCameraDialog(ivImagePreview, btnRemoveImage);
            } else {
                requestCameraPermission();
            }
        });

        btnChooseGallery.setOnClickListener(v -> {
            if (checkStoragePermission()) {
//                openGalleryForDialog(ivImagePreview, btnRemoveImage);
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

//        btnTakePhoto.setOnClickListener(v -> capturePhoto());
//
//        btnChooseGallery.setOnClickListener(v -> openGallery());
//
//        btnRemoveImage.setOnClickListener(v -> removeImage());

        btnRemoveImage.setOnClickListener(v -> {
            currentImageUri = null;
            ivImagePreview.setImageDrawable(null);
            ivImagePreview.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmitReport.setOnClickListener(v -> {
            String issueType = getSelectedIssueType(chipGroupIssueType);
            String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
            String contact = etContact.getText() != null ? etContact.getText().toString().trim() : "";
            if(description.isBlank() || contact.isBlank()) {
                Toast.makeText(VerificationHistoryActivity.this, "Fill in description (notes) and contact", Toast.LENGTH_SHORT).show();
                return;
            }
            submitIssueReport(item, issueType, description, contact, currentImageUri);
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

    private void submitIssueReport(VerificationHistory item, String issueType, String description, String contact, Uri imageUri) {
        Log.i(TAG, "Issue Report Submitted:");
        Log.i(TAG, "Serial: " + item.getSerial());
        Log.i(TAG, "Issue Type: " + issueType);
        Log.i(TAG, "Description: " + description);
        Log.i(TAG, "Contact: " + contact);
        Log.i(TAG, "Original Error: " + item.getErrorMessage());

        MultipartBody.Part photoPart = null;
        if (imageUri != null) {
            Log.i(TAG, "Image URI: " + imageUri.toString());
            try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] bytes = readBytes(inputStream);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
            photoPart = MultipartBody.Part.createFormData("photo", getFileName(imageUri), requestFile);

            } catch (Exception e) {
                Toast.makeText(this, "Failed to parse image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "submitIssueReport: Something went wrong -> ", e);
                return;
            }
        }

        RequestBody contactBody = RequestBody.create(MediaType.parse("text/plain"), contact);
        RequestBody descriptionBody = RequestBody.create(MediaType.parse("text/plain"), description);

        KeyService service = ApiClient.getRetrofit().create(KeyService.class);
        Call<ReportResponse> call = service.addReport(contactBody, descriptionBody, photoPart);

        call.enqueue(new Callback<ReportResponse>() {
            @Override
            public void onResponse(Call<ReportResponse> call, Response<ReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(VerificationHistoryActivity.this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    try (ResponseBody errorBody = response.errorBody()){
                        if (errorBody != null) {
                            Log.e(TAG, "onResponse: Error: " + errorBody.string());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse: Error: ", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ReportResponse> call, Throwable throwable) {
                Log.e(TAG, "onFailure: Failed network request", throwable);
                Toast.makeText(VerificationHistoryActivity.this, "Failed Network Request", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private String getFileName(Uri uri) {
        String result = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                result = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return result != null ? result : "upload_" + System.currentTimeMillis();
    }
    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }




    // Permission checking methods
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    // Camera dialog for image capture
    private void showCameraDialog(ImageView targetImageView, MaterialButton removeButton) {
        View cameraDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_camera_capture, null);
        PreviewView previewView = cameraDialogView.findViewById(R.id.previewView);
        MaterialButton btnCapture = cameraDialogView.findViewById(R.id.btnCapture);
        MaterialButton btnCancelCamera = cameraDialogView.findViewById(R.id.btnCancelCamera);

        cameraDialog = new AlertDialog.Builder(this)
                .setView(cameraDialogView)
                .setCancelable(false)
                .create();


        // Initialize camera for dialog
        setupCameraForDialog(previewView);

        btnCapture.setOnClickListener(v -> {
            capturePhotoForDialog(targetImageView, removeButton);
            cameraDialog.dismiss();
        });

        btnCancelCamera.setOnClickListener(v -> cameraDialog.dismiss());

        cameraDialog.show();
    }

    private void setupCameraForDialog(PreviewView previewView) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder();
                imageCapture = imageCaptureBuilder.build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhotoForDialog(ImageView targetImageView, MaterialButton removeButton) {
        if (imageCapture == null) return;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_" + timeStamp + ".jpg");
        currentImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@androidx.annotation.NonNull ImageCapture.OutputFileResults outputFileResults) {
                        runOnUiThread(() -> {
                            Toast.makeText(VerificationHistoryActivity.this, "Photo captured successfully", Toast.LENGTH_SHORT).show();
                            Glide.with(VerificationHistoryActivity.this)
                                    .load(currentImageUri)
                                    .into(targetImageView);
                            targetImageView.setVisibility(View.VISIBLE);
                            removeButton.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        runOnUiThread(() -> {
                            Toast.makeText(VerificationHistoryActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void openGalleryForDialog(ImageView targetImageView, MaterialButton removeButton) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        dialogGalleryLauncher.launch(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (verificationHistoryService != null) {
            verificationHistoryService.dispose();
        }
        disposables.clear();
        cameraExecutor.shutdown();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private enum FilterType {
        ALL, VERIFIED, FAILED
    }
}
