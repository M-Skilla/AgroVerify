package com.group.agroverify.database.repository;

import android.content.Context;
import android.util.Log;

import com.group.agroverify.database.VerificationDatabase;
import com.group.agroverify.database.dao.VerificationHistoryDao;
import com.group.agroverify.database.entities.VerificationHistory;
import com.group.agroverify.dtos.ScanEventResponse;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class VerificationHistoryRepository {

    private static final String TAG = "VerificationHistoryRepository";
    private VerificationHistoryDao verificationHistoryDao;
    private static volatile VerificationHistoryRepository INSTANCE;

    private VerificationHistoryRepository(Context context) {
        VerificationDatabase database = VerificationDatabase.getInstance(context);
        verificationHistoryDao = database.verificationHistoryDao();
    }

    public static VerificationHistoryRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (VerificationHistoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new VerificationHistoryRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public Completable saveSuccessfulVerification(String serial, String msisdn,
                                                 ScanEventResponse response) {
        return Completable.fromAction(() -> {
            VerificationHistory history = new VerificationHistory(
                serial,
                msisdn,
                System.currentTimeMillis(),
                true, // verification successful
                response.getId(),
                response.getTimestamp(),
                response.getIp(),
                response.getUser_agent()
            );

            Log.d(TAG, "Saving successful verification for serial: " + serial);
        }).andThen(verificationHistoryDao.insertVerification(
            new VerificationHistory(
                serial,
                msisdn,
                System.currentTimeMillis(),
                true,
                response.getId(),
                response.getTimestamp(),
                response.getIp(),
                response.getUser_agent()
            )
        )).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable saveFailedVerification(String serial, String msisdn,
                                            String errorMessage) {
        return Completable.fromAction(() -> {
            Log.d(TAG, "Saving failed verification for serial: " + serial + ", error: " + errorMessage);
        }).andThen(verificationHistoryDao.insertVerification(
            new VerificationHistory(
                serial,
                msisdn,
                System.currentTimeMillis(),
                false, // verification failed
                errorMessage
            )
        )).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<List<VerificationHistory>> getAllVerifications() {
        return verificationHistoryDao.getAllVerifications()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<List<VerificationHistory>> getVerificationsBySerial(String serial) {
        return verificationHistoryDao.getVerificationsBySerial(serial)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<List<VerificationHistory>> getSuccessfulVerifications() {
        return verificationHistoryDao.getVerificationsByResult(true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<List<VerificationHistory>> getFailedVerifications() {
        return verificationHistoryDao.getVerificationsByResult(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Integer> getTotalVerificationCount() {
        return verificationHistoryDao.getTotalVerificationCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Integer> getSuccessfulVerificationCount() {
        return verificationHistoryDao.getVerificationCountByResult(true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Integer> getFailedVerificationCount() {
        return verificationHistoryDao.getVerificationCountByResult(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable deleteOldVerifications(long olderThanTimestamp) {
        return verificationHistoryDao.deleteOldVerifications(olderThanTimestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable clearAllHistory() {
        return verificationHistoryDao.deleteAllVerifications()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
