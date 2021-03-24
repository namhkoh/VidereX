package com.kohdev.viderex;

import android.content.Context;
import android.content.Intent;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    String[] routeNames;
    Context context;

    public MyAdapter(Context context, String routeNames[]) {
        this.context = context;
        this.routeNames = routeNames;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.my_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.routeNameTv.setText(routeNames[position]);
//        holder.routeNameTv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(context)
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return routeNames.length;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView routeNameTv;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            routeNameTv = itemView.findViewById(R.id.route_text);
        }
    }
}
