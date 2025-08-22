package com.group.agroverify.service;

import android.content.Context;
import android.util.Log;

import com.group.agroverify.database.repository.VerificationHistoryRepository;
import com.group.agroverify.database.entities.VerificationHistory;
import com.group.agroverify.dtos.ScanEventResponse;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class VerificationHistoryService {

    private static final String TAG = "VerificationHistoryService";
    private VerificationHistoryRepository repository;
    private CompositeDisposable disposables;

    public VerificationHistoryService(Context context) {
        this.repository = VerificationHistoryRepository.getInstance(context);
        this.disposables = new CompositeDisposable();
    }

    /**
     * Records a successful verification result
     * This should be called when QR scan is successful and verification passes
     */
    public void recordSuccessfulVerification(String serial, String msisdn, ScanEventResponse response) {
        disposables.add(
            repository.saveSuccessfulVerification(serial, msisdn, response)
                .subscribe(
                    () -> Log.d(TAG, "Successfully recorded verification for serial: " + serial),
                    throwable -> Log.e(TAG, "Error recording successful verification", throwable)
                )
        );
    }

    /**
     * Records a failed verification result
     * This should be called when QR scan is successful but verification fails
     * Note: Malformed QR codes should NOT be recorded as per requirements
     */
    public void recordFailedVerification(String serial, String msisdn, String errorMessage) {
        disposables.add(
            repository.saveFailedVerification(serial, msisdn, errorMessage)
                .subscribe(
                    () -> Log.d(TAG, "Successfully recorded failed verification for serial: " + serial),
                    throwable -> Log.e(TAG, "Error recording failed verification", throwable)
                )
        );
    }

    /**
     * Get all verification history
     */
    public Single<List<VerificationHistory>> getAllVerificationHistory() {
        return repository.getAllVerifications();
    }

    /**
     * Get verification history for a specific product serial
     */
    public Single<List<VerificationHistory>> getVerificationHistoryBySerial(String serial) {
        return repository.getVerificationsBySerial(serial);
    }

    /**
     * Get only successful verifications
     */
    public Single<List<VerificationHistory>> getSuccessfulVerifications() {
        return repository.getSuccessfulVerifications();
    }

    /**
     * Get only failed verifications
     */
    public Single<List<VerificationHistory>> getFailedVerifications() {
        return repository.getFailedVerifications();
    }

    /**
     * Get verification statistics
     */
    public Single<VerificationStats> getVerificationStats() {
        return Single.zip(
            repository.getTotalVerificationCount(),
            repository.getSuccessfulVerificationCount(),
            repository.getFailedVerificationCount(),
            (total, successful, failed) -> new VerificationStats(total, successful, failed)
        );
    }

    /**
     * Clean up old verification records (older than specified days)
     */
    public void cleanupOldVerifications(int daysToKeep) {
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        disposables.add(
            repository.deleteOldVerifications(cutoffTime)
                .subscribe(
                    () -> Log.d(TAG, "Successfully cleaned up old verifications"),
                    throwable -> Log.e(TAG, "Error cleaning up old verifications", throwable)
                )
        );
    }

    /**
     * Clear all verification history
     */
    public void clearAllHistory() {
        disposables.add(
            repository.clearAllHistory()
                .subscribe(
                    () -> Log.d(TAG, "Successfully cleared all verification history"),
                    throwable -> Log.e(TAG, "Error clearing verification history", throwable)
                )
        );
    }

    /**
     * Dispose of all RxJava subscriptions to prevent memory leaks
     */
    public void dispose() {
        if (disposables != null && !disposables.isDisposed()) {
            disposables.clear();
        }
    }

    /**
     * Inner class for verification statistics
     */
    public static class VerificationStats {
        private final int totalVerifications;
        private final int successfulVerifications;
        private final int failedVerifications;

        public VerificationStats(int total, int successful, int failed) {
            this.totalVerifications = total;
            this.successfulVerifications = successful;
            this.failedVerifications = failed;
        }

        public int getTotalVerifications() {
            return totalVerifications;
        }

        public int getSuccessfulVerifications() {
            return successfulVerifications;
        }

        public int getFailedVerifications() {
            return failedVerifications;
        }

        public double getSuccessRate() {
            return totalVerifications > 0 ? (double) successfulVerifications / totalVerifications * 100 : 0;
        }
    }
}
