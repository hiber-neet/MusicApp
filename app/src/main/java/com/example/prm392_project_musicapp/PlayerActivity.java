package com.example.prm392_project_musicapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager; // Import AudioManager
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.SeekBar; // Import SeekBar
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.prm392_project_musicapp.service.MusicService;

import java.util.Collections;

public class PlayerActivity extends AppCompatActivity {

    private TextView songTitle, currentTime, totalTime;
    private Button btnPlayPause, btnNext, btnPrev, btnRepeat;
    private SeekBar seekBar;

    // Add these lines for volume control
    private SeekBar volumeSeekBar;
    private AudioManager audioManager;

    private MusicService musicService;
    private boolean isBound = false;

    private Handler handler = new Handler();
    private String songPath, title;

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (musicService != null && musicService.isPlaying()) {
                int pos = musicService.getCurrentPosition();
                seekBar.setProgress(pos);
                currentTime.setText(formatTime(pos));
            }
            handler.postDelayed(this, 500);
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Phát nhạc
            musicService.setPlaylist(Collections.singletonList(songPath), 0);
            musicService.play();

            // Cập nhật UI
            int duration = musicService.getDuration();
            seekBar.setMax(duration);
            totalTime.setText(formatTime(duration));
            handler.post(updateSeekBar);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        songTitle = findViewById(R.id.playerSongTitle);
        currentTime = findViewById(R.id.playerCurrentTime);
        totalTime = findViewById(R.id.playerTotalTime);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnRepeat = findViewById(R.id.btnRepeat);
        seekBar = findViewById(R.id.playerSeekBar);

        // Initialize volume control views and AudioManager
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Set up the volume SeekBar
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Called when the user starts dragging the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optional: Called when the user stops dragging the SeekBar
            }
        });


        Intent intent = getIntent();
        songPath = intent.getStringExtra("SONG_PATH");
        title = intent.getStringExtra("SONG_TITLE");

        songTitle.setText(title != null ? title : "Unknown");

        // Khởi động và bind service
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.putExtra("SONG_PATH", songPath);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        btnPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                    btnPlayPause.setText("Play");
                } else {
                    musicService.resume();
                    btnPlayPause.setText("Pause");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (musicService != null) musicService.next();
        });
        btnPrev.setOnClickListener(v -> {
            if (musicService != null) musicService.previous();
        });
        btnRepeat.setOnClickListener(v -> {
            if (musicService != null) musicService.toggleRepeat();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) {
                    musicService.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        handler.removeCallbacks(updateSeekBar);
    }

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
