package com.kohdev.viderex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RouteListViewActivity extends AppCompatActivity {

    private ArrayAdapter adapter;
    private ListView routeOptions;

    String json;
    String routeName;
    ArrayList<Uri> framePathList = new ArrayList<Uri>();
    ArrayList<String> routeNameList = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list_view);

        framePathList = (ArrayList<Uri>) getIntent().getSerializableExtra("uriList");
        json = (String) getIntent().getSerializableExtra("route_json");
        //routeNameList.add(json);
        System.out.println(framePathList);
        System.out.println(json);
        JSONObject obj = null;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            routeName = obj.getString("name");
            routeNameList.add(routeName);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //Used to automatically update the list on the screen.
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, routeNameList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView item = (TextView) super.getView(position, convertView, parent);
                item.setTextColor(Color.parseColor("#000000"));
                item.setTypeface(item.getTypeface(), Typeface.BOLD);
                item.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                item.setAlpha(0.7f);
                return item;
            }
        };
        routeOptions = findViewById(R.id.route_list);
        routeOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) adapter.getItem(position);
                System.out.println(item);
                for (int i = 0; i < routeOptions.getChildCount(); i++) {
                    if (position == i) {
                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#8c8c8c"));
                    } else {
                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#f2f2f2"));
                    }
                }

                // Find the correct route with name
                if (item.equals(routeName)) {
                    Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
                    intent.putExtra("route_json", json);
                    intent.putExtra("image_path", framePathList);
                    startActivity(intent);
                }
            }
        });
        routeOptions.setAdapter(adapter);
    }

}