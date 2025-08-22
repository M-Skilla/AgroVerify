package com.group.agroverify.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.group.agroverify.R;
import com.group.agroverify.database.entities.VerificationHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VerificationHistoryAdapter extends RecyclerView.Adapter<VerificationHistoryAdapter.ViewHolder> {

    private final List<VerificationHistory> historyList;
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(VerificationHistory item);
        void onReportIssueClick(VerificationHistory item);
    }

    public VerificationHistoryAdapter(List<VerificationHistory> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_verification_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VerificationHistory item = historyList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardVerificationItem;
        private final View statusIndicator;
        private final TextView tvSerial;
        private final ImageView ivStatusIcon;
        private final TextView tvStatus;
        private final TextView tvTimestamp;
        private final View errorContainer;
        private final TextView tvErrorMessage;
        private final MaterialButton actionButton;
        private final MaterialCardView successBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardVerificationItem = itemView.findViewById(R.id.cardVerificationItem);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            tvSerial = itemView.findViewById(R.id.tvSerial);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            errorContainer = itemView.findViewById(R.id.errorContainer);
            tvErrorMessage = itemView.findViewById(R.id.tvErrorMessage);
            actionButton = itemView.findViewById(R.id.actionButton);
            successBadge = itemView.findViewById(R.id.successBadge);
        }

        public void bind(VerificationHistory item, OnItemClickListener listener) {
            // Set serial number
            tvSerial.setText(item.getSerial());

            // Set timestamp
            Date scanDate = new Date(item.getScanTimestamp());
            tvTimestamp.setText(dateFormat.format(scanDate));

            // Configure UI based on verification result
            if (item.isVerificationResult()) {
                // Successful verification
                setupSuccessfulVerification();
            } else {
                // Failed verification
                setupFailedVerification(item);
            }

            // Set click listeners
            cardVerificationItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            actionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportIssueClick(item);
                }
            });
        }

        private void setupSuccessfulVerification() {
            // Status indicator - green
            statusIndicator.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_green_dark));

            // Status icon and text
            ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
            ivStatusIcon.setColorFilter(itemView.getContext().getColor(android.R.color.holo_green_dark));
            tvStatus.setText("Verification Successful");
            tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));

            // Hide error container and action button
            errorContainer.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);

            // Show success badge
            successBadge.setVisibility(View.VISIBLE);
        }

        private void setupFailedVerification(VerificationHistory item) {
            // Status indicator - red
            statusIndicator.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_red_dark));

            // Status icon and text
            ivStatusIcon.setImageResource(R.drawable.ic_error);
            ivStatusIcon.setColorFilter(itemView.getContext().getColor(android.R.color.holo_red_dark));
            tvStatus.setText("Verification Failed");
            tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));

            // Show error container with message
            errorContainer.setVisibility(View.VISIBLE);
            tvErrorMessage.setText(item.getErrorMessage() != null ? item.getErrorMessage() : "Unknown error occurred");

            // Show action button for reporting
            actionButton.setVisibility(View.VISIBLE);

            // Hide success badge
            successBadge.setVisibility(View.GONE);
        }
    }
}
