package com.tonpackage.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Playlist.class, Musique.class, Artiste.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MusicDao musicDao();

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "TheAppDB.db")
                    .createFromAsset("TheAppDB.db")
                    .fallbackToDestructiveMigration() // On utilise ça pour simplifier le dev si on change souvent
                    .addMigrations(MIGRATION_1_3, MIGRATION_2_3, MIGRATION_3_4)
                    .build();
        }
        return instance;
    }

    static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Musique ADD COLUMN IsPodcast INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE Musique ADD COLUMN IdArtiste INTEGER");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Musique ADD COLUMN IdArtiste INTEGER");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Playlist ADD COLUMN SelectedColor INTEGER");
        }
    };
}