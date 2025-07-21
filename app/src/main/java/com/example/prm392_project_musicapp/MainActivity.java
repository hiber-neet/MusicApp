package com.example.prm392_project_musicapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392_project_musicapp.adapter.SongAdapter;
import com.example.prm392_project_musicapp.data.MusicDatabaseHelper;
import com.example.prm392_project_musicapp.data.Song;
import com.example.prm392_project_musicapp.utils.MusicScanner;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private Button btnCreateSong;
    private MusicDatabaseHelper dbHelper;
    private SongAdapter adapter;
    private ArrayList<Song> songList = new ArrayList<>();

    private static final int REQUEST_PICK_AUDIO = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Xin quyền (Android 12 & 13)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.READ_MEDIA_AUDIO"}, 1);
            } catch (Exception e) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        dbHelper = new MusicDatabaseHelper(this);
        recyclerView = findViewById(R.id.songRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchBar = findViewById(R.id.searchBar);
        btnCreateSong = findViewById(R.id.btnCreateSong);
        String musicDir = "/storage/emulated/0/Music/";
        MediaScannerConnection.scanFile(this,
                new String[]{ musicDir },
                null,
                (path, uri) -> {
                    System.out.println("Scanned: " + path + " -> " + uri);
                });
        // Quét nhạc sẵn có từ MediaStore vào DB
        MusicScanner.scanAndStore(this);

        // Load danh sách nhạc
        loadSongs();

        // Chọn nhạc thủ công (Create New Song)
        btnCreateSong.setOnClickListener(v -> openFilePicker());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_AUDIO && resultCode == RESULT_OK && data != null) {
            Uri audioUri = data.getData();
            if (audioUri != null) {
                savePickedSong(audioUri);
                loadSongs();
            }
        }
    }

    private void savePickedSong(Uri uri) {
        String title = "Unknown";
        String displayName = "";
        String artist = "Unknown";

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                displayName = cursor.getString(nameIndex);
                title = displayName.replace(".mp3", "").replace(".wav", "");
            }
            cursor.close();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("artist", artist);
        values.put("album", "Custom");
        values.put("path", uri.toString());
        values.put("duration", 0);
        db.insert("songs", null, values);
        db.close();
    }

    private void loadSongs() {
        songList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM songs ORDER BY id DESC", null);
        if (c != null) {
            while (c.moveToNext()) {
                songList.add(new Song(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("title")),
                        c.getString(c.getColumnIndexOrThrow("artist")),
                        c.getString(c.getColumnIndexOrThrow("album")),
                        c.getString(c.getColumnIndexOrThrow("path")),
                        c.getInt(c.getColumnIndexOrThrow("duration"))
                ));
            }
            c.close();
        }
        db.close();

        adapter = new SongAdapter(songList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSongClick(Song song) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("SONG_PATH", song.getPath());
        startActivity(intent);
    }
}
