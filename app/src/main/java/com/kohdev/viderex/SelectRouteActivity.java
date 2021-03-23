package com.kohdev.viderex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

public class SelectRouteActivity extends AppCompatActivity {

    String[] routeNames;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_route);

        routeNames = getResources().getStringArray(R.array.RoutNames);
        recyclerView = findViewById(R.id.RouteRecyclerView);

        MyAdapter myAdapter = new MyAdapter(this, routeNames);
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }
}