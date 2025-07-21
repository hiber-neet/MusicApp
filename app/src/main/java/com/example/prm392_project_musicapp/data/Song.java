package com.example.prm392_project_musicapp.data;

public class Song {
    public int id;
    public String title;
    public String artist;
    public String album;
    public String path;
    public int duration;

    public Song(int id, String title, String artist, String album, String path, int duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.duration = duration;
    }
    public String getTitle(){
        return this.title;
    }
    public String getArtist(){
        return this.artist;
    }
    public String getPath(){
        return this.path;
    }
}
