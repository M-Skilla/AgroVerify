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
import com.group.agroverify.dtos.PublicKeyRequest;
import com.group.agroverify.dtos.ScanEventRequest;
import com.group.agroverify.dtos.ScanEventResponse;
import com.group.agroverify.models.Keystore;
import com.group.agroverify.service.KeyService;
import com.group.agroverify.service.VerificationHistoryService;
import com.group.agroverify.utils.ApiClient;
import com.group.agroverify.utils.KeyUtils;
import com.group.agroverify.utils.LocaleHelper;
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

    // Verification history service
    private VerificationHistoryService verificationHistoryService;

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
        // Simulate processing delay
        new Thread(() -> {
            String extractedSerialId = null;
            try {
                String kid = KeyUtils.extractKid(qrContent.trim());
                String signature = KeyUtils.extractSignature(qrContent.trim());
                String data = KeyUtils.extractData(qrContent.trim());

                // Try to extract serial ID early for error tracking
                try {
                    JSONObject payload = KeyUtils.extractPayload(qrContent.trim());
                    extractedSerialId = payload.optString("serial", null);
                } catch (Exception e) {
                    Log.w(TAG, "Could not extract serial ID for error tracking", e);
                }

                KeyService service = ApiClient.getRetrofit().create(KeyService.class);
                Call<Keystore> call = service.getPublicKeyByKid(new PublicKeyRequest(kid));

                Log.d(TAG, "processQrCode: Before API Call");
                Log.i(TAG, "processQrCode: Data: " + data);
                Log.i(TAG, "processQrCode: Signature: " + signature);

                // Create final variable for use in callbacks
                final String finalExtractedSerialId = extractedSerialId;

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

                                boolean legit = KeyUtils.verifySignature(data, signature, publicKey);

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
                                        handleVerificationError(
                                                getString(R.string.error_not_agro_input),
                                                getString(R.string.error_not_agro_input_message),
                                                finalExtractedSerialId
                                        );
                                        return;
                                    }
                                }

                                // Create final variables for lambda use
                                final boolean isLegit = legit;
                                final String finalSerialId = serialId;
                                final String finalProductId = productId;
                                final String finalBatchId = batchId;

                                // Record verification result using real API call
                                if (finalSerialId != null) {
                                    // Get phone number for scan event
                                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                                    String phoneNumber = "unknown";
                                    if (telephonyManager != null) {
                                        try {
                                            phoneNumber = telephonyManager.getLine1Number();
                                            if (phoneNumber == null || phoneNumber.isEmpty()) {
                                                phoneNumber = "unknown";
                                            }
                                        } catch (SecurityException e) {
                                            Log.w(TAG, "No permission to read phone number", e);
                                            phoneNumber = "unknown";
                                        }
                                    }

                                    // Make real API call to addScanEvent
                                    final String finalPhoneNumber = phoneNumber;
                                    ScanEventRequest scanRequest = new ScanEventRequest(finalSerialId, finalPhoneNumber);
                                    Call<ScanEventResponse> scanCall = service.addScanEvent(scanRequest);

                                    scanCall.enqueue(new Callback<ScanEventResponse>() {
                                        @Override
                                        public void onResponse(Call<ScanEventResponse> call, Response<ScanEventResponse> response) {
                                            if (response.isSuccessful() && response.body() != null) {
                                                Log.d(TAG, "Scan event recorded successfully");
                                                ScanEventResponse scanResponse = response.body();

                                                if (isLegit) {
                                                    // Record successful verification in local database
                                                    verificationHistoryService.recordSuccessfulVerification(
                                                        finalSerialId,
                                                        scanResponse.getMsisdn(),
                                                        scanResponse
                                                    );
                                                } else {
                                                    // Record failed verification (valid QR structure but signature verification failed)
                                                    verificationHistoryService.recordFailedVerification(
                                                        finalSerialId,
                                                        scanResponse.getMsisdn(),
                                                        "Signature verification failed - invalid or tampered product"
                                                    );
                                                }
                                            } else {
                                                // API call failed - likely means product/batch doesn't exist (counterfeit)
                                                Log.e(TAG, "addScanEvent failed - Response code: " + response.code());
                                                String errorMsg = "Product or batch not found in database - likely counterfeit";

                                                // Record failed verification for counterfeit product
                                                verificationHistoryService.recordFailedVerification(
                                                    finalSerialId,
                                                    finalPhoneNumber,
                                                    errorMsg
                                                );

                                                // Override verification result to show as counterfeit
                                                runOnUiThread(() -> {
                                                    showLoadingOverlay(false);
                                                    scanCount++;
                                                    updateScanCount();
                                                    updateScanStatus("Ready to scan");

                                                    // Show counterfeit product dialog
                                                    showVerificationDialog(false, finalSerialId, finalProductId, finalBatchId,
                                                        "Counterfeit Product", "This product was not found in our database and is likely counterfeit.");
                                                });
                                                return;
                                            }

                                            // Show normal verification result
                                            runOnUiThread(() -> {
                                                showLoadingOverlay(false);
                                                scanCount++;
                                                updateScanCount();
                                                updateScanStatus("Ready to scan");

                                                Log.d(TAG, "processQrCode: IS LEGIT = " + isLegit);
                                                showVerificationDialog(isLegit, finalSerialId, finalProductId, finalBatchId, null, null);
                                            });
                                        }

                                        @Override
                                        public void onFailure(Call<ScanEventResponse> call, Throwable throwable) {
                                            Log.e(TAG, "addScanEvent API call failed", throwable);

                                            // Record failed verification due to network issue
                                            verificationHistoryService.recordFailedVerification(
                                                finalSerialId,
                                                finalPhoneNumber,
                                                "Network error during scan event recording: " + throwable.getMessage()
                                            );

                                            // Show verification result anyway, but with a warning
                                            runOnUiThread(() -> {
                                                showLoadingOverlay(false);
                                                scanCount++;
                                                updateScanCount();
                                                updateScanStatus("Ready to scan");

                                                // Show verification with network error warning
                                                String warningMsg = isLegit ?
                                                    "Signature verified, but could not verify product in database due to network error." :
                                                    "Invalid signature detected.";
                                                showVerificationDialog(isLegit, finalSerialId, finalProductId, finalBatchId,
                                                    "Network Error", warningMsg);
                                            });
                                        }
                                    });
                                } else {
                                    // No serial ID available, just show result without recording
                                    runOnUiThread(() -> {
                                        showLoadingOverlay(false);
                                        scanCount++;
                                        updateScanCount();
                                        updateScanStatus("Ready to scan");

                                        Log.d(TAG, "processQrCode: IS LEGIT = " + isLegit);
                                        showVerificationDialog(isLegit, finalSerialId, finalProductId, finalBatchId, null, null);
                                    });
                                }
                            } else {
                                // API call failed or returned empty response
                                Log.e(TAG, "Failed to fetch Public Key - Response code: " + response.code());
                                handleVerificationError(
                                        getString(R.string.error_server_verification),
                                        getString(R.string.error_signature_verification_message),
                                        finalExtractedSerialId
                                );
                            }
                        } catch (Exception e) {
                            // Error during signature verification or key loading
                            Log.e(TAG, "Error during signature verification", e);
                            handleVerificationError(
                                    getString(R.string.error_signature_verification),
                                    getString(R.string.error_signature_verification_message),
                                    finalExtractedSerialId
                            );
                        }
                    }

                    @Override
                    public void onFailure(Call<Keystore> call, Throwable throwable) {
                        // Network or API call failure
                        Log.e(TAG, "API call failed", throwable);
                        handleVerificationError(
                                getString(R.string.error_network_failure),
                                getString(R.string.error_network_failure_message),
                                finalExtractedSerialId
                        );
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing QR code - invalid token format", e);
                handleVerificationError(
                        getString(R.string.error_not_agro_input),
                        getString(R.string.error_not_agro_input_message),
                        extractedSerialId
                );
            }
        }).start();
    }

    private void toggleFlashlight() {
        if (camera != null && hasFlash) {
            try {
                // Use CameraX torch control
                camera.getCameraControl().enableTorch(!isFlashlightOn);
                isFlashlightOn = !isFlashlightOn;
                updateFlashlightButton();

                String message = isFlashlightOn ? "Flashlight on" : "Flashlight off";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Failed to toggle flashlight", e);
                Toast.makeText(this, "Failed to toggle flashlight", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFlashlightButton() {
        if (isFlashlightOn) {
            btnFlashlight.setIconResource(R.drawable.flashlight_on);
            btnFlashlight.setIconTintResource(R.color.md_theme_primary);
        } else {
            btnFlashlight.setIconResource(R.drawable.flashlight_off);
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

    private void showVerificationDialog(boolean legit, String serialId, String productId, String batchId, String errorTitle, String errorMsg) {
        ProductVerificationDialog dialog = ProductVerificationDialog.newInstance(legit, serialId, productId, batchId, errorTitle, errorMsg);

        // Set up dismiss callback to re-enable scanning only when dialog is dismissed
        dialog.setOnDismissCallback(() -> {
            Log.d(TAG, "Verification dialog dismissed - re-enabling scanning");
            isScanning = true;
        });

        dialog.show(getSupportFragmentManager(), "verification_dialog");
    }

    /**
     * Handles verification errors by showing the invalid product dialog and recording failed verification
     * @param errorTitle The error title to display
     * @param errorMessage The error message to log and display
     */
    private void handleVerificationError(String errorTitle, String errorMessage) {
        handleVerificationError(errorTitle, errorMessage, null);
    }

    /**
     * Handles verification errors by showing the invalid product dialog and recording failed verification
     * @param errorTitle The error title to display
     * @param errorMessage The error message to log and display
     * @param serialId The serial ID if available for recording failed verification
     */
    private void handleVerificationError(String errorTitle, String errorMessage, String serialId) {
        Log.e(TAG, "Verification error: " + errorMessage);

        // Record failed verification if we have a serial ID
        if (serialId != null && !serialId.isEmpty()) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String phoneNumber = "unknown";
                if (telephonyManager != null) {
                    try {
                        phoneNumber = telephonyManager.getLine1Number();
                        if (phoneNumber == null || phoneNumber.isEmpty()) {
                            phoneNumber = "unknown";
                        }
                    } catch (SecurityException e) {
                        Log.w(TAG, "No permission to read phone number", e);
                        phoneNumber = "unknown";
                    }
                }

                verificationHistoryService.recordFailedVerification(
                    serialId,
                    phoneNumber,
                    errorMessage
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to record failed verification", e);
            }
        }

        runOnUiThread(() -> {
            showLoadingOverlay(false);
            updateScanStatus("Ready to scan");

            // Show invalid product dialog for any verification error
            showVerificationDialog(false, serialId, null, null, errorTitle, errorMessage);

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
        if (isFlashlightOn && camera != null) {
            try {
                camera.getCameraControl().enableTorch(false);
                isFlashlightOn = false;
                updateFlashlightButton();
            } catch (Exception e) {
                Log.e(TAG, "Failed to turn off flashlight", e);
            }
        }
    }
}
