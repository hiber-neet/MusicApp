package com.example.prm392_project_musicapp;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392_project_musicapp.adapter.SongAdapter;
import com.example.prm392_project_musicapp.data.MusicDatabaseHelper;
import com.example.prm392_project_musicapp.data.Song;
import com.example.prm392_project_musicapp.service.MusicService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongAdapter.OnSongLongClickListener {

    public static final String EXTRA_PLAYLIST_ID = "playlist_id";
    public static final String EXTRA_PLAYLIST_NAME = "playlist_name";
    private MusicDatabaseHelper dbHelper;
    private RecyclerView songsRecyclerView;
    private TextView playlistTitle, emptyStateMessage;
    private ImageView btnBack, btnMenu;
    private List<Song> songs;
    private SongAdapter adapter;
    private int playlistId;
    private String playlistName;
    private ImageButton btnPlayPause, btnNext, btnPrev;
    private SeekBar volumeSeekBar;
    private AudioManager audioManager;
    private MusicService musicService;
    private boolean isBound = false;
    private Handler handler = new Handler();
    private ArrayList<String> songPaths;
    private int currentIndex = 0;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            if (!songs.isEmpty()) {
                setupSongPaths();
                musicService.setPlaylist(songPaths, currentIndex);
                musicService.play();
                btnPlayPause.setImageResource(R.drawable.ic_pause_circle);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        Intent intent = getIntent();
        playlistId = intent.getIntExtra(EXTRA_PLAYLIST_ID, -1);
        playlistName = intent.getStringExtra(EXTRA_PLAYLIST_NAME);

        if (playlistId == -1 || playlistName == null) {
            Toast.makeText(this, "Error loading playlist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupMusicControls();
        loadPlaylistSongs();
    }

    private void initViews() {
        dbHelper = new MusicDatabaseHelper(this);
        songsRecyclerView = findViewById(R.id.songsRecyclerView);
        playlistTitle = findViewById(R.id.playlistTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);

        playlistTitle.setText(playlistName);
        btnBack.setOnClickListener(v -> finish());
        btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void setupMusicControls() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play_circle);
                } else {
                    musicService.resume();
                    btnPlayPause.setImageResource(R.drawable.ic_pause_circle);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < songs.size() - 1 && musicService != null) {
                currentIndex++;
                playSongAtCurrentIndex();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0 && musicService != null) {
                currentIndex--;
                playSongAtCurrentIndex();
            }
        });
    }

    private void setupSongPaths() {
        songPaths = new ArrayList<>();
        for (Song song : songs) {
            songPaths.add(song.getPath());
        }
    }

    private void playSongAtCurrentIndex() {
        if (musicService != null && !songs.isEmpty()) {
            musicService.setPlaylist(songPaths, currentIndex);
            musicService.play();
            btnPlayPause.setImageResource(R.drawable.ic_pause_circle);

            adapter.notifyDataSetChanged();
        }
    }

    private void startMusicService() {
        if (!songs.isEmpty()) {
            Intent serviceIntent = new Intent(this, MusicService.class);
            startService(serviceIntent);
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_playlist_detail, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete_playlist) {
                showDeleteConfirmationDialog();
                return true;
            } else if (itemId == R.id.action_rename_playlist) {
                showUpdateNameDialog();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete \"" + playlistName + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePlaylist())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpdateNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_name, null);

        TextInputEditText etNewPlaylistName = dialogView.findViewById(R.id.etNewPlaylistName);
        Button btnCancelDialog = dialogView.findViewById(R.id.btnCancelDialog);
        Button btnCreateDialog = dialogView.findViewById(R.id.btnCreateDialog);

        etNewPlaylistName.setText(playlistName);
        etNewPlaylistName.setSelection(playlistName.length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancelDialog.setOnClickListener(v -> dialog.dismiss());

        btnCreateDialog.setOnClickListener(v -> {
            String newName = etNewPlaylistName.getText().toString().trim();

            if (newName.isEmpty()) {
                etNewPlaylistName.setError("Playlist name cannot be empty");
                return;
            }

            if (newName.equals(playlistName)) {
                dialog.dismiss();
                return;
            }

            updatePlaylistName(newName);
            dialog.dismiss();
        });
        dialog.show();
        etNewPlaylistName.requestFocus();
    }

    private void deletePlaylist() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.delete("playlist_songs", "playlist_id = ?", new String[]{String.valueOf(playlistId)});

            int deletedRows = db.delete("playlists", "id = ?", new String[]{String.valueOf(playlistId)});

            if (deletedRows > 0) {
                Toast.makeText(this, "Playlist deleted successfully", Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("deleted_playlist_id", playlistId);
                resultIntent.putExtra("action", "delete");
                setResult(RESULT_OK, resultIntent);

                finish();
            } else {
                Toast.makeText(this, "Failed to delete playlist", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error deleting playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private void updatePlaylistName(String newName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("name", newName);

            int updatedRows = db.update("playlists", values, "id = ?", new String[]{String.valueOf(playlistId)});

            if (updatedRows > 0) {
                playlistName = newName;
                playlistTitle.setText(newName);
                Toast.makeText(this, "Playlist renamed successfully", Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("updated_playlist_id", playlistId);
                resultIntent.putExtra("updated_playlist_name", newName);
                resultIntent.putExtra("action", "rename");
                setResult(RESULT_OK, resultIntent);
            } else {
                Toast.makeText(this, "Failed to rename playlist", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error updating playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private void setupRecyclerView() {
        songs = new ArrayList<>();
        adapter = new SongAdapter(songs, this, this);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songsRecyclerView.setAdapter(adapter);
    }

    private void loadPlaylistSongs() {
        songs.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT s.id, s.title, s.artist, s.album, s.path, s.duration " +
                "FROM songs s " +
                "INNER JOIN playlist_songs ps ON s.id = ps.song_id " +
                "WHERE ps.playlist_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(playlistId)});

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String title = cursor.getString(1);
            String artist = cursor.getString(2);
            String album = cursor.getString(3);
            String path = cursor.getString(4);
            int duration = cursor.getInt(5);

            songs.add(new Song(id, title, artist, album, path, duration));
        }

        cursor.close();
        db.close();

        if (songs.isEmpty()) {
            emptyStateMessage.setVisibility(View.VISIBLE);
            songsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateMessage.setVisibility(View.GONE);
            songsRecyclerView.setVisibility(View.VISIBLE);
            startMusicService();
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSongClick(Song song) {
        // Find the clicked song index and play it
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() == song.getId()) {
                currentIndex = i;
                playSongAtCurrentIndex();
                break;
            }
        }
    }

    @Override
    public void onSongLongClick(Song song) {
        Toast.makeText(this, "Long clicked: " + song.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        handler.removeCallbacksAndMessages(null);
    }
}