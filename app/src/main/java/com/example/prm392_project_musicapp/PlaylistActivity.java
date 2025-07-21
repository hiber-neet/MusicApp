package com.example.prm392_project_musicapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.prm392_project_musicapp.R;
import com.example.prm392_project_musicapp.data.MusicDatabaseHelper;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {

    private MusicDatabaseHelper dbHelper;
    private ListView playlistListView;
    private Button btnAddPlaylist, btnDeletePlaylist;
    private EditText inputPlaylistName;

    private ArrayList<String> playlists;
    private ArrayAdapter<String> adapter;

    private int selectedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        dbHelper = new MusicDatabaseHelper(this);
        playlistListView = findViewById(R.id.playlistListView);
        btnAddPlaylist = findViewById(R.id.btnAddPlaylist);
        btnDeletePlaylist = findViewById(R.id.btnDeletePlaylist);
        inputPlaylistName = findViewById(R.id.inputPlaylistName);

        playlists = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_activated_1, playlists);
        playlistListView.setAdapter(adapter);

        loadPlaylists();

        playlistListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedIndex = position;
            playlistListView.setItemChecked(position, true);
        });

        btnAddPlaylist.setOnClickListener(v -> addPlaylist());
        btnDeletePlaylist.setOnClickListener(v -> deletePlaylist());
    }

    private void loadPlaylists() {
        playlists.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM playlists", null);
        while (c.moveToNext()) {
            playlists.add(c.getString(0));
        }
        c.close();
        db.close();
        adapter.notifyDataSetChanged();
    }

    private void addPlaylist() {
        String name = inputPlaylistName.getText().toString().trim();
        if (name.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        db.insert("playlists", null, values);
        db.close();

        inputPlaylistName.setText("");
        loadPlaylists();
    }

    private void deletePlaylist() {
        if (selectedIndex == -1) return;

        String name = playlists.get(selectedIndex);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("playlists", "name=?", new String[]{name});
        db.close();

        selectedIndex = -1;
        loadPlaylists();
    }
}
