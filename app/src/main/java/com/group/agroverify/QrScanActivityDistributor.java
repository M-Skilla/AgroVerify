package com.group.agroverify;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.group.agroverify.dtos.BatchAssignmentRequest;
import com.group.agroverify.dtos.BatchAssignmentResponse;
import com.group.agroverify.dtos.PublicKeyRequest;
import com.group.agroverify.dtos.ScanEventRequest;
import com.group.agroverify.dtos.ScanEventResponse;
import com.group.agroverify.models.Keystore;
import com.group.agroverify.service.DistributorService;
import com.group.agroverify.service.KeyService;
import com.group.agroverify.service.VerificationHistoryService;
import com.group.agroverify.utils.ApiClient;
import com.group.agroverify.utils.KeyUtils;
import com.group.agroverify.utils.LocaleHelper;
import com.group.agroverify.fragment.ProductVerificationDialog;

import org.json.JSONObject;

import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrScanActivityDistributor extends AppCompatActivity {

    private static final String TAG = "QrScanActivityDistributor";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private PreviewView previewView;
    private MaterialButton btnFlashlight;
    private TextView tvScanStatus;
    private TextView tvScanCount;
    private MaterialCardView loadingOverlay;

    // Camera and ML Kit
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean hasFlash = false;

    // State
    private boolean isFlashlightOn = false;
    private boolean isScanning = true;
    private int scanCount = 0;

    // Services
    private DistributorService distributorService;
    private VerificationHistoryService verificationHistoryService;

    // Distributor functionality
    private int distributorId = -1;
    private String distributorName = "";

    // Activity Result Launcher for gallery
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_scan);

        // Get distributor info from intent
        Intent intent = getIntent();
        distributorId = intent.getIntExtra("distributorId", -1);
        distributorName = intent.getStringExtra("distributorName");

        initializeActivityResultLaunchers();
        initializeComponents();
        setupClickListeners();
        initializeCamera();
        initializeBarcodeScanner();
    }

    private void initializeActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Toast.makeText(this, "Gallery image selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initializeComponents() {
        // Find views
        previewView = new PreviewView(this);
        FrameLayout cameraContainer = findViewById(R.id.cameraPreviewContainer);
        if (cameraContainer != null) {
            cameraContainer.addView(previewView, 0);
        }

        btnFlashlight = findViewById(R.id.btnFlashlight);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvScanCount = findViewById(R.id.tvScanCount);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Initialize camera components
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        // Initialize services
        distributorService = ApiClient.getRetrofit().create(DistributorService.class);
        verificationHistoryService = new VerificationHistoryService(this);

        // Update initial state
        updateScanCount();
        updateScanStatus("Ready to scan batch QR codes");
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Flashlight toggle
        btnFlashlight.setOnClickListener(v -> toggleFlashlight());

        // Gallery button
        findViewById(R.id.btnGallery).setOnClickListener(v -> openGallery());

        // Manual entry button
        findViewById(R.id.btnManualEntry).setOnClickListener(v -> openManualEntry());
    }

    private void initializeCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    private void initializeBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image analysis use case for QR scanning
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (isScanning) {
                @SuppressWarnings("UnsafeOptInUsageError")
                android.media.Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    InputImage image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.getImageInfo().getRotationDegrees());

                    barcodeScanner.process(image)
                            .addOnSuccessListener(this::processBarcodes)
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Barcode scanning failed", e);
                            })
                            .addOnCompleteListener(task -> imageProxy.close());
                } else {
                    imageProxy.close();
                }
            } else {
                imageProxy.close();
            }
        });

        // Camera selector
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera and store camera instance
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            // Check if camera has flash capability
            hasFlash = camera.getCameraInfo().hasFlashUnit();

            // Get camera ID for fallback flashlight control
            getCameraId();

        } catch (Exception e) {
            Log.e(TAG, "Failed to bind camera use cases", e);
            Toast.makeText(this, "Failed to bind camera use cases: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void getCameraId() {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length > 0) {
                cameraId = cameraIds[0]; // Use back camera (usually index 0)

                // Check if the camera has flash capability
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to get camera ID", e);
        }
    }

    private void processBarcodes(java.util.List<Barcode> barcodes) {
        for (Barcode barcode : barcodes) {
            if (barcode.getFormat() == Barcode.FORMAT_QR_CODE && isScanning) {
                String qrContent = barcode.getRawValue();
                if (qrContent != null && !qrContent.isEmpty()) {
                    runOnUiThread(() -> {
                        isScanning = false;
                        showLoadingOverlay(true);
                        updateScanStatus("QR code detected");
                        processQrCode(qrContent);
                    });
                    break;
                }
            }
        }
    }

    private void processQrCode(String qrContent) {
        // For distributor mode, handle batch assignment
        if (distributorId != -1) {
            processBatchAssignment(qrContent);
        } else {
            // Show error message if no distributor logged in
            runOnUiThread(() -> {
                showLoadingOverlay(false);
                updateScanStatus("No distributor logged in");
                Toast.makeText(this, "Please log in as a distributor first", Toast.LENGTH_LONG).show();

                // Re-enable scanning
                tvScanStatus.postDelayed(() -> {
                    isScanning = true;
                    updateScanStatus("Ready to scan batch QR codes");
                }, 2000);
            });
        }
    }

    private void processBatchAssignment(String batchId) {
        // Extract batch ID from QR content
        String cleanBatchId = extractBatchId(batchId);

        // Create timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Create batch assignment request
        BatchAssignmentRequest request = new BatchAssignmentRequest(distributorId, cleanBatchId, timestamp);

        // Make API call to assign batch
        Call<BatchAssignmentResponse> call = distributorService.assignBatch(request);
        call.enqueue(new Callback<BatchAssignmentResponse>() {
            @Override
            public void onResponse(Call<BatchAssignmentResponse> call, Response<BatchAssignmentResponse> response) {
                runOnUiThread(() -> {
                    showLoadingOverlay(false);

                    if (response.isSuccessful() && response.body() != null) {
                        BatchAssignmentResponse assignmentResponse = response.body();

                        if (assignmentResponse.isSuccess()) {
                            // Success
                            scanCount++;
                            updateScanCount();
                            updateScanStatus("Batch assigned successfully!");

                            Toast.makeText(QrScanActivityDistributor.this,
                                         "Batch " + cleanBatchId + " assigned to " + distributorName,
                                         Toast.LENGTH_LONG).show();
                        } else {
                            // Assignment failed
                            updateScanStatus("Batch assignment failed");
                            Toast.makeText(QrScanActivityDistributor.this,
                                         "Failed to assign batch: " + assignmentResponse.getMessage(),
                                         Toast.LENGTH_LONG).show();
                        }
                    } else {
                        updateScanStatus("Assignment failed");
                        Toast.makeText(QrScanActivityDistributor.this,
                                     "Failed to assign batch. Please try again.",
                                     Toast.LENGTH_LONG).show();
                    }

                    // Re-enable scanning after a delay
                    tvScanStatus.postDelayed(() -> {
                        isScanning = true;
                        updateScanStatus("Ready to scan batch QR codes");
                    }, 2000);
                });
            }

            @Override
            public void onFailure(Call<BatchAssignmentResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    showLoadingOverlay(false);
                    updateScanStatus("Network error");
                    Toast.makeText(QrScanActivityDistributor.this,
                                 "Network error: " + t.getMessage(),
                                 Toast.LENGTH_LONG).show();

                    // Re-enable scanning after a delay
                    tvScanStatus.postDelayed(() -> {
                        isScanning = true;
                        updateScanStatus("Ready to scan batch QR codes");
                    }, 2000);
                });
            }
        });
    }

    private String extractBatchId(String qrContent) {
        // Simple extraction - can be enhanced based on QR code format
        try {
            // Try to parse as JSON first
            JSONObject json = new JSONObject(qrContent);
            if (json.has("batchId")) {
                return json.getString("batchId");
            } else if (json.has("batch_id")) {
                return json.getString("batch_id");
            } else if (json.has("batch")) {
                return json.getString("batch");
            }
        } catch (Exception e) {
            Log.d(TAG, "QR content is not JSON, using as direct batch ID");
        }

        // If not JSON or no batch field found, use the content as is
        return qrContent.trim();
    }

    private void toggleFlashlight() {
        if (hasFlash && camera != null) {
            try {
                if (camera.getCameraControl() != null) {
                    camera.getCameraControl().enableTorch(!isFlashlightOn);
                    isFlashlightOn = !isFlashlightOn;
                    updateFlashlightButton();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling flashlight", e);
                Toast.makeText(this, "Error toggling flashlight", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFlashlightButton() {
        if (btnFlashlight != null) {
            btnFlashlight.setAlpha(isFlashlightOn ? 1.0f : 0.6f);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openManualEntry() {
        // Implement manual batch ID entry if needed
        Toast.makeText(this, "Manual entry not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void updateScanCount() {
        if (tvScanCount != null) {
            tvScanCount.setText("Batches scanned: " + scanCount);
        }
    }

    private void updateScanStatus(String status) {
        if (tvScanStatus != null) {
            tvScanStatus.setText(status);
        }
    }

    private void showLoadingOverlay(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for scanning", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }
}
