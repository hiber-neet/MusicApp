package com.example.prm392_project_musicapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392_project_musicapp.R;
import com.example.prm392_project_musicapp.data.Song;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public interface OnSongLongClickListener {
        void onSongLongClick(Song song);
    }

    private List<Song> songs;
    private OnSongClickListener clickListener;
    private OnSongLongClickListener longClickListener;

    public SongAdapter(List<Song> songs, OnSongClickListener clickListener, OnSongLongClickListener longClickListener) {
        this.songs = songs;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        holder.itemView.setOnClickListener(v -> clickListener.onSongClick(song));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onSongLongClick(song);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.songTitle);
            artist = v.findViewById(R.id.songArtist);
        }
    }
}
