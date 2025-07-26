package com.example.prm392_project_musicapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392_project_musicapp.R;
import com.example.prm392_project_musicapp.data.Playlist;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public interface OnPlaylistLongClickListener {
        void onPlaylistLongClick(Playlist playlist);
    }

    private List<Playlist> playlists;
    private OnPlaylistClickListener clickListener;
    private OnPlaylistLongClickListener longClickListener;

    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener clickListener, OnPlaylistLongClickListener longClickListener) {
        this.playlists = playlists;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.name.setText(playlist.getName());
        
        // Set song count
        int songCount = playlist.getSongCount();
        if (songCount == 1) {
            holder.songCount.setText("1 song");
        } else {
            holder.songCount.setText(songCount + " songs");
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlaylistClick(playlist);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPlaylistLongClick(playlist);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, songCount;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.playlistName);
            songCount = v.findViewById(R.id.playlistSongCount);
        }
    }
}
