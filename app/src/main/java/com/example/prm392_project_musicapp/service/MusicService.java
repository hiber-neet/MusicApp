package com.example.prm392_project_musicapp.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<String> playlist = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRepeat = false;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Nếu có bài hát truyền qua khi startService
        if (intent != null && intent.hasExtra("SONG_PATH")) {
            String songPath = intent.getStringExtra("SONG_PATH");
            setPlaylist(new ArrayList<String>() {{ add(songPath); }}, 0);
            play();
        }
        return START_STICKY;
    }

    public void setPlaylist(List<String> songs, int startIndex) {
        this.playlist = songs;
        this.currentIndex = startIndex;
    }

    public void play() {
        if (playlist == null || playlist.isEmpty()) return;
        String path = playlist.get(currentIndex);

        releasePlayer();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(path));  // Hỗ trợ content:// và file://
            mediaPlayer.setOnCompletionListener(mp -> {
                if (isRepeat) {
                    play();  // lặp lại cùng bài
                } else {
                    next();  // tự động sang bài tiếp
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            releasePlayer();
        }
    }

    public void next() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex + 1) % playlist.size();
        play();
    }

    public void previous() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        play();
    }

    public void toggleRepeat() {
        isRepeat = !isRepeat;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int ms) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(ms);
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
}
