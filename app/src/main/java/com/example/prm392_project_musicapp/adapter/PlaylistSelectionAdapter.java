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

public class PlaylistSelectionAdapter extends RecyclerView.Adapter<PlaylistSelectionAdapter.ViewHolder> {

    public interface OnPlaylistSelectionListener {
        void onPlaylistSelected(Playlist playlist);
    }

    private List<Playlist> playlists;
    private OnPlaylistSelectionListener listener;

    public PlaylistSelectionAdapter(List<Playlist> playlists, OnPlaylistSelectionListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.textView.setText(playlist.getName());
        holder.textView.setTextColor(0xFFFFFFFF); // White text
        holder.textView.setPadding(32, 16, 32, 16);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistSelected(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View v) {
            super(v);
            textView = (TextView) v;
        }
    }
} 