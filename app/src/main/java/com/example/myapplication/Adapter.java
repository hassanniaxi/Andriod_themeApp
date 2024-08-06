package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class Adapter extends RecyclerView.Adapter<Adapter.MyHolder> {

    private final String[] titles;
    private final int[] images;

    public Adapter(String[] titles, int[] images) {
        this.titles = titles;
        this.images = images;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.data_layer, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.wallCoverTitle.setText(titles[position]);
        holder.wallCover.setImageResource(images[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView wallCover;
        TextView wallCoverTitle;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            wallCover = itemView.findViewById(R.id.wall_covers);
            wallCoverTitle = itemView.findViewById(R.id.wall_cover_titles);
        }
    }
}
