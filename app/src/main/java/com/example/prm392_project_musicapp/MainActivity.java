package com.example.prm392_project_musicapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392_project_musicapp.adapter.PlaylistAdapter;
import com.example.prm392_project_musicapp.adapter.PlaylistSelectionAdapter;
import com.example.prm392_project_musicapp.adapter.SongAdapter;
import com.example.prm392_project_musicapp.data.MusicDatabaseHelper;
import com.example.prm392_project_musicapp.data.Playlist;
import com.example.prm392_project_musicapp.data.Song;
import com.example.prm392_project_musicapp.utils.MusicScanner;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongAdapter.OnSongLongClickListener, SongAdapter.OnSongMoreOptionsClickListener {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private Button btnCreateSong;
    private Button btnOpenPlaylist;
    private VideoView backgroundVideo;

    private MusicDatabaseHelper dbHelper;
    private SongAdapter adapter;
    private ArrayList<Song> songList = new ArrayList<>();
    private ArrayList<Song> filteredList = new ArrayList<>();

    private static final int REQUEST_PICK_AUDIO = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Xin quyền đọc file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.READ_MEDIA_AUDIO"}, 1);
            } catch (Exception e) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        // Khởi tạo các view
        dbHelper = new MusicDatabaseHelper(this);
        recyclerView = findViewById(R.id.songRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchBar = findViewById(R.id.searchBar);
        btnCreateSong = findViewById(R.id.btnCreateSong);
        btnOpenPlaylist = findViewById(R.id.btnOpenPlaylists);
        backgroundVideo = findViewById(R.id.backgroundVideo);

        playBackgroundVideo(); // ✅ Gọi khi khởi tạo

        // Quét nhạc trong thư mục
        String musicDir = "/storage/emulated/0/Music/";
        MediaScannerConnection.scanFile(this, new String[]{musicDir}, null, null);

        MusicScanner.scanAndStore(this);
        loadSongs();

        // Tìm kiếm
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSongs(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Mở file nhạc
        btnCreateSong.setOnClickListener(v -> openFilePicker());

        // Mở danh sách playlist
        btnOpenPlaylist.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlaylistActivity.class);
            startActivity(intent);
        });
    }

    // ✅ Gọi lại phát video khi quay lại màn hình
    @Override
    protected void onResume() {
        super.onResume();
        playBackgroundVideo();
    }

    // ✅ Dừng video khi app bị che (quay đi màn hình khác)
    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundVideo != null && backgroundVideo.isPlaying()) {
            backgroundVideo.pause();
        }
    }

    private void playBackgroundVideo() {
        if (backgroundVideo != null) {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bg_loop);
            backgroundVideo.setVideoURI(uri);
            backgroundVideo.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
                backgroundVideo.start();
            });
        }
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

        filteredList.clear();
        filteredList.addAll(songList);
        adapter = new SongAdapter(filteredList, this, this);
        adapter.setMoreOptionsClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void filterSongs(String query) {
        filteredList.clear();
        for (Song s : songList) {
            if (s.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    s.getArtist().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(s);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSongClick(Song song) {
        int clickedIndex = filteredList.indexOf(song); // Lấy vị trí của bài hát trong filteredList
        ArrayList<String> paths = new ArrayList<>();
        for (Song s : filteredList) {
            paths.add(s.getPath());
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("SONG_LIST", paths);
        intent.putExtra("SONG_INDEX", clickedIndex);
        intent.putExtra("SONG_TITLE", song.getTitle()); // Optional: để hiển thị sớm
        startActivity(intent);
    }

    @Override
    public void onSongLongClick(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete \"" + song.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("songs", "id = ?", new String[]{String.valueOf(song.getId())});
                    db.close();
                    loadSongs();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onSongMoreOptionsClick(Song song) {
        showPlaylistSelectionDialog(song);
    }

    private void showPlaylistSelectionDialog(Song song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_playlist, null);
        builder.setView(dialogView);

        TextView tvSongInfo = dialogView.findViewById(R.id.tvSongInfo);
        RecyclerView playlistRecyclerView = dialogView.findViewById(R.id.playlistRecyclerView);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnCreateNew = dialogView.findViewById(R.id.btnCreateNew);

        tvSongInfo.setText("Song: " + song.getTitle() + " - " + song.getArtist());
        AlertDialog dialog = builder.create();
        // Load playlists
        ArrayList<Playlist> playlists = loadPlaylists();
        PlaylistSelectionAdapter playlistAdapter = new PlaylistSelectionAdapter(playlists, playlist -> {
            addSongToPlaylist(song, playlist);
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistRecyclerView.setAdapter(playlistAdapter);



        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreateNew.setOnClickListener(v -> {
            dialog.dismiss();
            showCreatePlaylistDialog(song);
        });

        dialog.show();
    }

    private ArrayList<Playlist> loadPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM playlists ORDER BY name", null);
        if (c != null) {
            while (c.moveToNext()) {
                playlists.add(new Playlist(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name"))
                ));
            }
            c.close();
        }
        db.close();
        return playlists;
    }

    private void addSongToPlaylist(Song song, Playlist playlist) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Check if song is already in playlist
        Cursor c = db.rawQuery("SELECT * FROM playlist_songs WHERE playlist_id = ? AND song_id = ?", 
                new String[]{String.valueOf(playlist.getId()), String.valueOf(song.getId())});
        
        if (c != null && c.getCount() > 0) {
            Toast.makeText(this, "Song is already in this playlist", Toast.LENGTH_SHORT).show();
            c.close();
        } else {
            if (c != null) c.close();
            
            ContentValues values = new ContentValues();
            values.put("playlist_id", playlist.getId());
            values.put("song_id", song.getId());
            db.insert("playlist_songs", null, values);
            
            Toast.makeText(this, "Added to playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();
        }
        db.close();
    }

    private void showCreatePlaylistDialog(Song song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Playlist");

        final EditText input = new EditText(this);
        input.setHint("Enter playlist name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                createPlaylistAndAddSong(playlistName, song);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createPlaylistAndAddSong(String playlistName, Song song) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Create playlist
        ContentValues playlistValues = new ContentValues();
        playlistValues.put("name", playlistName);
        long playlistId = db.insert("playlists", null, playlistValues);
        
        if (playlistId != -1) {
            // Add song to playlist
            ContentValues songValues = new ContentValues();
            songValues.put("playlist_id", playlistId);
            songValues.put("song_id", song.getId());
            db.insert("playlist_songs", null, songValues);
            
            Toast.makeText(this, "Created playlist and added song: " + playlistName, Toast.LENGTH_SHORT).show();
        }
        
        db.close();
    }
}
