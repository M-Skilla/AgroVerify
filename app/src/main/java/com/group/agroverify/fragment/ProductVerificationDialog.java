package com.group.agroverify.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.group.agroverify.R;
import com.group.agroverify.dtos.ScanEventRequest;
import com.group.agroverify.dtos.ScanEventResponse;
import com.group.agroverify.service.KeyService;
import com.group.agroverify.utils.ApiClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductVerificationDialog extends DialogFragment {
    private static final String TAG = "ProductVerificationDialog";

    private static final String ARG_IS_VALID = "is_valid";
    private static final String ARG_SERIAL_ID = "serial_id";
    private static final String ARG_PRODUCT_ID = "product_id";
    private static final String ARG_BATCH_ID = "batch_id";

    private boolean isValid;
    private String serialId;
    private String productId;
    private String batchId;

    private LottieAnimationView animationView;
    private TextView titleText;
    private TextView statusText;
    private View detailsContainer;
    private TextView serialIdText;
    private TextView productIdText;
    private TextView batchIdText;

    // Callback interface for dismiss events
    public interface OnDismissCallback {
        void onDialogDismissed();
    }

    private OnDismissCallback dismissCallback;

    public static ProductVerificationDialog newInstance(boolean isValid, String serialId, String productId, String batchId) {
        ProductVerificationDialog dialog = new ProductVerificationDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_VALID, isValid);
        args.putString(ARG_SERIAL_ID, serialId);
        args.putString(ARG_PRODUCT_ID, productId);
        args.putString(ARG_BATCH_ID, batchId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDismissCallback(OnDismissCallback callback) {
        this.dismissCallback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isValid = getArguments().getBoolean(ARG_IS_VALID);
            serialId = getArguments().getString(ARG_SERIAL_ID);
            productId = getArguments().getString(ARG_PRODUCT_ID);
            batchId = getArguments().getString(ARG_BATCH_ID);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_product_verification, null);
        initializeViews(dialogView);
        setupDialog();

        builder.setView(dialogView);
        Dialog dialog = builder.create();
        dialog.setCancelable(false);

        return dialog;
    }

    private void initializeViews(View view) {
        animationView = view.findViewById(R.id.animationView);
        titleText = view.findViewById(R.id.titleText);
        statusText = view.findViewById(R.id.statusText);
        detailsContainer = view.findViewById(R.id.detailsContainer);
        serialIdText = view.findViewById(R.id.serialIdText);
        productIdText = view.findViewById(R.id.productIdText);
        batchIdText = view.findViewById(R.id.batchIdText);

        MaterialButton closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
    }

    private void setupDialog() {
        if (isValid) {
            setupValidProduct();
            addScanEvent();
        } else {
            setupInvalidProduct();
        }
    }

    private void setupValidProduct() {
        // Set success animation
        animationView.setAnimation(R.raw.check_success);
        animationView.playAnimation();

        // Set success texts
        titleText.setText(R.string.product_verified_title);
        statusText.setText(R.string.product_verified_status);

        // Show product details
        detailsContainer.setVisibility(View.VISIBLE);
        serialIdText.setText(getString(R.string.serial_id_format, serialId != null ? serialId : getString(R.string.not_available)));
        productIdText.setText(getString(R.string.product_id_format, productId != null ? productId : getString(R.string.not_available)));
        batchIdText.setText(getString(R.string.batch_id_format, batchId != null ? batchId : getString(R.string.not_available)));
    }

    private void setupInvalidProduct() {
        // Set error animation
        animationView.setAnimation(R.raw.error);
        animationView.playAnimation();

        // Set error texts
        titleText.setText(R.string.product_invalid_title);
        statusText.setText(R.string.product_invalid_status);

        // Hide product details for invalid products
        detailsContainer.setVisibility(View.GONE);
    }

    private void addScanEvent() {
        if (serialId == null || serialId.isEmpty()) {
            Log.w(TAG, "Cannot add scan event - serial ID is null or empty");
            return;
        }

        KeyService keyService = ApiClient.getRetrofit().create(KeyService.class);

        String msisdn = "0000000000"; // You might want to get this from device or user preferences

        ScanEventRequest request = new ScanEventRequest(serialId, msisdn);

        Call<ScanEventResponse> call = keyService.addScanEvent(request);
        call.enqueue(new Callback<ScanEventResponse>() {
            @Override
            public void onResponse(@NonNull Call<ScanEventResponse> call, @NonNull Response<ScanEventResponse> response) {
                try (ResponseBody errBody = response.errorBody()) {
                    Log.d(TAG, "onResponse: Message: " + (errBody != null ? errBody.string() : ""));
                } catch (Exception e) {
                    Log.e(TAG, "onResponse: ", e);
                }
                if (response.isSuccessful()) {
                    Log.d(TAG, "Scan event added successfully");
                } else {
                    Log.e(TAG, "Failed to add scan event: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ScanEventResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error adding scan event", t);
            }
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(TAG, "Dialog dismissed - notifying callback");
        if (dismissCallback != null) {
            dismissCallback.onDialogDismissed();
        }
    }
}
