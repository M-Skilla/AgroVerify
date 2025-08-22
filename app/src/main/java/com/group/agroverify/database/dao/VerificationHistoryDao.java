package com.group.agroverify.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import com.group.agroverify.database.entities.VerificationHistory;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface VerificationHistoryDao {

    @Insert
    Completable insertVerification(VerificationHistory verificationHistory);

    @Query("SELECT * FROM verification_history ORDER BY scan_timestamp DESC")
    Single<List<VerificationHistory>> getAllVerifications();

    @Query("SELECT * FROM verification_history WHERE serial = :serial ORDER BY scan_timestamp DESC")
    Single<List<VerificationHistory>> getVerificationsBySerial(String serial);

    @Query("SELECT * FROM verification_history WHERE verification_result = :isSuccess ORDER BY scan_timestamp DESC")
    Single<List<VerificationHistory>> getVerificationsByResult(boolean isSuccess);

    @Query("SELECT * FROM verification_history WHERE scan_timestamp BETWEEN :startTime AND :endTime ORDER BY scan_timestamp DESC")
    Single<List<VerificationHistory>> getVerificationsByDateRange(long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM verification_history")
    Single<Integer> getTotalVerificationCount();

    @Query("SELECT COUNT(*) FROM verification_history WHERE verification_result = :isSuccess")
    Single<Integer> getVerificationCountByResult(boolean isSuccess);

    @Query("DELETE FROM verification_history WHERE scan_timestamp < :timestamp")
    Completable deleteOldVerifications(long timestamp);

    @Delete
    Completable deleteVerification(VerificationHistory verificationHistory);

    @Query("DELETE FROM verification_history")
    Completable deleteAllVerifications();
}
