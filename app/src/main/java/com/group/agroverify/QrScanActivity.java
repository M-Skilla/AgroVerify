package com.group.agroverify;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
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
import com.group.agroverify.dtos.PublicKeyRequest;
import com.group.agroverify.dtos.ScanEventResponse;
import com.group.agroverify.models.Keystore;
import com.group.agroverify.service.KeyService;
import com.group.agroverify.service.VerificationHistoryService;
import com.group.agroverify.utils.ApiClient;
import com.group.agroverify.utils.KeyUtils;
import com.group.agroverify.fragment.ProductVerificationDialog;

import org.json.JSONObject;

import java.security.PublicKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrScanActivity extends AppCompatActivity {

    private static final String TAG = "QrScanActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private PreviewView previewView;
    private MaterialButton btnFlashlight;
    private TextView tvScanStatus;
    private TextView tvScanCount;
    private MaterialCardView loadingOverlay;

    // Camera and ML Kit
    private ProcessCameraProvider cameraProvider;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private CameraManager cameraManager;
    private String cameraId;

    // State
    private boolean isFlashlightOn = false;
    private boolean isScanning = true;
    private int scanCount = 0;

    // Verification history service
    private VerificationHistoryService verificationHistoryService;

    // Activity Result Launcher for gallery
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_scan);

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
                        // Handle gallery image selection
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

        // Initialize verification history service
        verificationHistoryService = new VerificationHistoryService(this);

        // Update initial state
        updateScanCount();
        updateScanStatus("Ready to scan");
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

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            // Get camera ID for flashlight control
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
        // Simulate processing delay
        new Thread(() -> {
            try {


                String kid = KeyUtils.extractKid(qrContent.trim());
                String signature = KeyUtils.extractSignature(qrContent.trim());
                String data = KeyUtils.extractData(qrContent.trim());
                KeyService service = ApiClient.getRetrofit().create(KeyService.class);
                Call<Keystore> call = service.getPublicKeyByKid(new PublicKeyRequest(kid));

                Log.d(TAG, "processQrCode: Before API Call");
                Log.i(TAG, "processQrCode: Data: " + data);
                Log.i(TAG, "processQrCode: Signature: " + signature);

                call.enqueue(new Callback<Keystore>() {
                    @Override
                    public void onResponse(Call<Keystore> call, Response<Keystore> response) {
                        Log.d(TAG, "onResponse: API Response " + response.code());
                        Log.d(TAG, "onResponse: API Response " + response.message());
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                PublicKey publicKey = KeyUtils.loadPublicKey(response.body().getPublicKey());
                                System.out.println("Public Key" + publicKey);
                                Log.i(TAG, "onResponse: PublicKey" + publicKey);

                                // Perform verification after public key is loaded
                                boolean legit = KeyUtils.verifySignature(data, signature, publicKey);

                                // Extract product details from token payload
                                String serialId = null;
                                String productId = null;
                                String batchId = null;

                                if (legit) {
                                    try {
                                        JSONObject payload = KeyUtils.extractPayload(qrContent.trim());
                                        serialId = payload.optString("serial", null);
                                        productId = payload.optString("product_id", null);
                                        batchId = payload.optString("batch_id", null);
                                        Log.d(TAG, "Extracted payload - Serial: " + serialId + ", Product: " + productId + ", Batch: " + batchId);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to extract payload data", e);
                                    }
                                }

                                // Create final variables for lambda use
                                final boolean isLegit = legit;
                                final String finalSerialId = serialId;
                                final String finalProductId = productId;
                                final String finalBatchId = batchId;

                                // Record verification result in database
                                // Note: We only record verifications where we got structured data (serialId exists)
                                // This excludes malformed QR codes as per requirements
                                if (finalSerialId != null) {
                                    // Get phone number using proper TelephonyManager instance
                                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                                    String phoneNumber = "unknown";
                                    if (telephonyManager != null) {
                                        try {
                                            // Note: This requires READ_PHONE_STATE permission
                                            phoneNumber = telephonyManager.getLine1Number();
                                            if (phoneNumber == null || phoneNumber.isEmpty()) {
                                                phoneNumber = "unknown";
                                            }
                                        } catch (SecurityException e) {
                                            Log.w(TAG, "No permission to read phone number", e);
                                            phoneNumber = "unknown";
                                        }
                                    }

                                    // Create a mock ScanEventResponse for verification recording
                                    // In a real implementation, this would come from an actual scan event API call
                                    ScanEventResponse mockResponse = new ScanEventResponse(
                                        "local_" + System.currentTimeMillis(), // mock ID
                                        finalSerialId,
                                        String.valueOf(System.currentTimeMillis()), // current timestamp
                                        "local", // mock IP
                                        phoneNumber,
                                        "AgroVerify Android App"
                                    );

                                    if (isLegit) {
                                        // Record successful verification
                                        verificationHistoryService.recordSuccessfulVerification(
                                            finalSerialId,
                                            mockResponse.getMsisdn(),
                                            mockResponse
                                        );
                                    } else {
                                        // Record failed verification (valid QR structure but verification failed)
                                        verificationHistoryService.recordFailedVerification(
                                            finalSerialId,
                                            mockResponse.getMsisdn(),
                                            "Signature verification failed - invalid or tampered product"
                                        );
                                    }
                                }

                                runOnUiThread(() -> {
                                    showLoadingOverlay(false);
                                    scanCount++;
                                    updateScanCount();
                                    updateScanStatus("Ready to scan");

                                    // Show verification dialog
                                    Log.d(TAG, "processQrCode: IS LEGIT = " + isLegit);
                                    showVerificationDialog(isLegit, finalSerialId, finalProductId, finalBatchId);

                                    // Scanning will be re-enabled when dialog is dismissed
                                });
                            } else {
                                // API call failed or returned empty response
                                Log.e(TAG, "Failed to fetch Public Key - Response code: " + response.code());
                                handleVerificationError("Failed to fetch public key from server");
                            }
                        } catch (Exception e) {
                            // Error during signature verification or key loading
                            Log.e(TAG, "Error during signature verification", e);
                            handleVerificationError("Failed to verify product signature");
                        }
                    }

                    @Override
                    public void onFailure(Call<Keystore> call, Throwable throwable) {
                        // Network or API call failure
                        Log.e(TAG, "API call failed", throwable);
                        handleVerificationError("Network error - unable to verify product");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing QR code - invalid token format", e);
                handleVerificationError("Invalid QR code format");
            }
        }).start();
    }

    private void toggleFlashlight() {
        if (cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, !isFlashlightOn);
                isFlashlightOn = !isFlashlightOn;
                updateFlashlightButton();

                String message = isFlashlightOn ? "Flashlight on" : "Flashlight off";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to toggle flashlight", e);
                Toast.makeText(this, "Failed to toggle flashlight", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFlashlightButton() {
        if (isFlashlightOn) {
            btnFlashlight.setIconResource(R.drawable.ic_flashlight_on);
            btnFlashlight.setIconTintResource(R.color.md_theme_primary);
        } else {
            btnFlashlight.setIconResource(R.drawable.ic_flashlight_off);
            btnFlashlight.setIconTintResource(R.color.md_theme_onSurfaceVariant);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openManualEntry() {
        // TODO: Implement manual QR code entry dialog
        Toast.makeText(this, "Manual entry feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void updateScanStatus(String status) {
        if (tvScanStatus != null) {
            tvScanStatus.setText(status);
        }
    }

    private void updateScanCount() {
        if (tvScanCount != null) {
            tvScanCount.setText(String.valueOf(scanCount));
        }
    }

    private void showLoadingOverlay(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showVerificationDialog(boolean legit, String serialId, String productId, String batchId) {
        ProductVerificationDialog dialog = ProductVerificationDialog.newInstance(legit, serialId, productId, batchId);

        // Set up dismiss callback to re-enable scanning only when dialog is dismissed
        dialog.setOnDismissCallback(() -> {
            Log.d(TAG, "Verification dialog dismissed - re-enabling scanning");
            isScanning = true;
        });

        dialog.show(getSupportFragmentManager(), "verification_dialog");
    }

    /**
     * Handles verification errors by showing the invalid product dialog
     * @param errorMessage The error message to log
     */
    private void handleVerificationError(String errorMessage) {
        Log.e(TAG, "Verification error: " + errorMessage);

        runOnUiThread(() -> {
            showLoadingOverlay(false);
            updateScanStatus("Ready to scan");

            // Show invalid product dialog for any verification error
            showVerificationDialog(false, null, null, null);

            // Scanning will be re-enabled when dialog is dismissed via the dismiss listener
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
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
        if (verificationHistoryService != null) {
            verificationHistoryService.dispose();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Turn off flashlight when activity is paused
        if (isFlashlightOn && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false);
                isFlashlightOn = false;
                updateFlashlightButton();
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to turn off flashlight", e);
            }
        }
    }
}
