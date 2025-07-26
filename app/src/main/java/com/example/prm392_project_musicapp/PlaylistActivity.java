package com.example.prm392_project_musicapp;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392_project_musicapp.adapter.PlaylistAdapter;
import com.example.prm392_project_musicapp.data.MusicDatabaseHelper;
import com.example.prm392_project_musicapp.data.Playlist;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {

    private MusicDatabaseHelper dbHelper;
    private FloatingActionButton fabAddPlaylist;
    private RecyclerView playlistRecyclerView;
    private LinearLayout emptyStateLayout;
    private PlaylistAdapter adapter;
    private List<Playlist> playlists;
    private ActivityResultLauncher<Intent> playlistDetailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        dbHelper = new MusicDatabaseHelper(this);

        initViews();
        setupRecyclerView();
        setupActivityResultLauncher();
        loadPlaylists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh playlists when returning to this activity
        loadPlaylists();
    }

    private void initViews() {
        fabAddPlaylist = findViewById(R.id.fabAddPlaylist);
        playlistRecyclerView = findViewById(R.id.playlistRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        fabAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    private void setupRecyclerView() {
        playlists = new ArrayList<>();

        PlaylistAdapter.OnPlaylistClickListener clickListener = playlist -> {
            Intent intent = new Intent(this, PlaylistDetailActivity.class);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
            playlistDetailLauncher.launch(intent);
        };

        PlaylistAdapter.OnPlaylistLongClickListener longClickListener = playlist -> {
            // Handle long click if needed
        };

        adapter = new PlaylistAdapter(playlists, clickListener, longClickListener);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistRecyclerView.setAdapter(adapter);
    }

    private void setupActivityResultLauncher() {
        playlistDetailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String action = data.getStringExtra("action");
                            if ("delete".equals(action)) {
                                int deletedPlaylistId = data.getIntExtra("deleted_playlist_id", -1);
                                if (deletedPlaylistId != -1) {
                                    removePlaylistFromList(deletedPlaylistId);
                                    updateEmptyState();
                                }
                            } else if ("rename".equals(action)) {
                                int updatedPlaylistId = data.getIntExtra("updated_playlist_id", -1);
                                String updatedPlaylistName = data.getStringExtra("updated_playlist_name");
                                if (updatedPlaylistId != -1 && updatedPlaylistName != null) {
                                    updatePlaylistInList(updatedPlaylistId, updatedPlaylistName);
                                }
                            }
                        }
                    }
                });
    }

    private void updatePlaylistInList(int playlistId, String newName) {
        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getId() == playlistId) {
                playlists.get(i).setName(newName);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void loadPlaylists() {
        playlists.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to get playlists with song counts
        String query = "SELECT p.id, p.name, COUNT(ps.song_id) as song_count " +
                "FROM playlists p " +
                "LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id " +
                "GROUP BY p.id, p.name " +
                "ORDER BY p.name ASC";

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            int songCount = cursor.getInt(2);
            playlists.add(new Playlist(id, name, songCount));
        }

        cursor.close();
        db.close();

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyStateLayout != null) {
            if (playlists.isEmpty()) {
                emptyStateLayout.setVisibility(View.VISIBLE);
                playlistRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateLayout.setVisibility(View.GONE);
                playlistRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showCreatePlaylistDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_playlist);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText editPlaylistName = dialog.findViewById(R.id.editPlaylistName);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String playlistName = editPlaylistName.getText().toString().trim();
            if (!TextUtils.isEmpty(playlistName)) {
                createPlaylist(playlistName);
                dialog.dismiss();
            } else {
                editPlaylistName.setError("Please enter a playlist name");
            }
        });

        dialog.show();
    }

    private void createPlaylist(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);

        long id = db.insert("playlists", null, values);
        if (id != -1) {
            playlists.add(new Playlist((int) id, name, 0)); // New playlist has 0 songs
            adapter.notifyItemInserted(playlists.size() - 1);
            updateEmptyState();
            Toast.makeText(this, "Playlist created successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to create playlist. Name might already exist.", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }

    private void removePlaylistFromList(int playlistId) {
        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getId() == playlistId) {
                playlists.remove(i);
                adapter.notifyItemRemoved(i);
                updateEmptyState();
                break;
            }
        }
    }
}