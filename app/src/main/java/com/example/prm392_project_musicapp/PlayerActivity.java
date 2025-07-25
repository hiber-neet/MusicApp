package com.example.prm392_project_musicapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.prm392_project_musicapp.service.MusicService;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {

    private TextView songTitle, currentTime, totalTime;
    private ImageButton btnPlayPause, btnNext, btnPrev, btnRepeat;
    private SeekBar seekBar, volumeSeekBar;

    private AudioManager audioManager;
    private MusicService musicService;
    private boolean isBound = false;

    private Handler handler = new Handler();
    private ArrayList<String> songList;
    private int currentIndex;

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

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Phát danh sách
            musicService.setPlaylist(songList, currentIndex);
            musicService.play();

            // Cập nhật UI
            songTitle.setText(getFileName(songList.get(currentIndex)));
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
        volumeSeekBar = findViewById(R.id.volumeSeekBar);

        // Volume setup
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

        // Nhận dữ liệu
        Intent intent = getIntent();
        songList = intent.getStringArrayListExtra("SONG_LIST");
        currentIndex = intent.getIntExtra("CURRENT_INDEX", 0);

        if (songList == null || songList.isEmpty()) {
            finish();
            return;
        }

        songTitle.setText(getFileName(songList.get(currentIndex)));

        // Start service
        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        // Buttons
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
            if (currentIndex < songList.size() - 1) {
                currentIndex++;
                playSongAtCurrentIndex();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                playSongAtCurrentIndex();
            }
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

    private void playSongAtCurrentIndex() {
        if (musicService != null) {
            musicService.setPlaylist(songList, currentIndex);
            musicService.play();
            songTitle.setText(getFileName(songList.get(currentIndex)));
            int duration = musicService.getDuration();
            seekBar.setMax(duration);
            totalTime.setText(formatTime(duration));
            handler.post(updateSeekBar);
        }
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

    private String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1).replace(".mp3", "").replace(".wav", "");
    }
}
