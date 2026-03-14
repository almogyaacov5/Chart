package com.example.chart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DrawerNavAdapter extends RecyclerView.Adapter<DrawerNavAdapter.VH> {

    public interface Listener {
        void onClick(NavDrawerItem item);
    }

    private final List<NavDrawerItem> items;
    private final Listener listener;
    private int selectedId = 0;

    public DrawerNavAdapter(List<NavDrawerItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public List<NavDrawerItem> getItems() {
        return items;
    }

    public void setSelectedId(int id) {
        selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drawer_nav, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NavDrawerItem item = items.get(position);
        holder.txtTitle.setText(item.title);

        boolean selected = (item.id == selectedId);

        if (selected) {
            holder.root.setBackgroundResource(R.drawable.nav_item_selected_bg);
            holder.txtTitle.setTextColor(0xFF000000); // שחור מלא
            holder.txtTitle.setAlpha(1.0f);
        } else {
            holder.root.setBackgroundResource(R.drawable.nav_item_default_bg);
            holder.txtTitle.setTextColor(0xFF000000); // שחור
            holder.txtTitle.setAlpha(0.8f); // דהוי
        }


        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView txtTitle;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root);
            txtTitle = itemView.findViewById(R.id.txtTitle);
        }
    }
}
