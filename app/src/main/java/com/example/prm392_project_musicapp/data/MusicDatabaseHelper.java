package com.example.prm392_project_musicapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MusicDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "musicplayer.db";
    private static final int DB_VERSION = 2;

    public MusicDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE songs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "artist TEXT," +
                "album TEXT," +
                "path TEXT," +
                "duration INTEGER)");

        db.execSQL("CREATE TABLE playlists (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT)");

        db.execSQL("CREATE TABLE playlist_songs (" +
                "playlist_id INTEGER," +
                "song_id INTEGER," +
                "PRIMARY KEY (playlist_id, song_id)," +
                "FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE," +
                "FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE," +
                "password TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS playlist_songs");
        db.execSQL("DROP TABLE IF EXISTS playlists");
        db.execSQL("DROP TABLE IF EXISTS songs");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}
