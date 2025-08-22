package com.group.agroverify.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.group.agroverify.database.dao.VerificationHistoryDao;
import com.group.agroverify.database.entities.VerificationHistory;

@Database(
    entities = {VerificationHistory.class},
    version = 1,
    exportSchema = false
)
public abstract class VerificationDatabase extends RoomDatabase {

    private static volatile VerificationDatabase INSTANCE;
    private static final String DATABASE_NAME = "verification_database";

    public abstract VerificationHistoryDao verificationHistoryDao();

    public static VerificationDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (VerificationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            VerificationDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
