package com.example.prm392_project_musicapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.prm392_project_musicapp.data.MusicDatabaseHelper;

public class MusicScanner {
    public static void scanAndStore(Context context) {
        MusicDatabaseHelper helper = new MusicDatabaseHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("songs", null, null);

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };
        Cursor c = context.getContentResolver().query(uri, proj,
                MediaStore.Audio.Media.IS_MUSIC + "!=0", null, null);
        if (c != null) {
            while (c.moveToNext()) {
                ContentValues v = new ContentValues();
                v.put("title", c.getString(0));
                v.put("artist", c.getString(1));
                v.put("album", c.getString(2));
                v.put("path", c.getString(3));
                v.put("duration", c.getInt(4));
                db.insert("songs", null, v);
            }
            c.close();
        }
        db.close();
    }
}
