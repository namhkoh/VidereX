package com.kohdev.viderex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.ls.LSOutput;

import java.util.ArrayList;
import java.util.Map;

import io.grpc.internal.JsonUtil;

public class RouteListViewActivity extends AppCompatActivity {

    private ArrayAdapter adapter;
    private ListView routeOptions;

    String json;
    Map jsonMap;
    String routeName;
    ArrayList<Uri> framePathList = new ArrayList<Uri>();
    ArrayList<String> routeNameList = new ArrayList<String>();

    private DocumentReference mDocRef = FirebaseFirestore.getInstance().document("RouteObject/KZEdEXDKTP8Ag10X1TLa");
    // Source can be CACHE, SERVER, or DEFAULT.
    Source source = Source.CACHE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list_view);
//        Button fetch = findViewById(R.id.fetch);
//        fetch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                fetchRoutes();
//            }
//        });

        Button refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshRoutes();
            }
        });


//        framePathList = (ArrayList<Uri>) getIntent().getSerializableExtra("uriList");
//        json = (String) getIntent().getSerializableExtra("route_json");
//        //routeNameList.add(json);
//        System.out.println(framePathList);
//        System.out.println(json);
//        JSONObject obj = null;
//        try {
//            obj = new JSONObject(json);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        try {
//            routeName = obj.getString("name");
//            routeNameList.add(routeName);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }


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

//    private void refreshRoutes() {
//        mDocRef.get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                System.out.println("Entering land");
//                if (task.isSuccessful()) {
//                    DocumentSnapshot document = task.getResult();
//                    jsonMap = document.getData();
//                    Log.d("Document", "Cached document data: " + document.getData());
//                } else {
//                    Log.d("Document", "Cached document failed: " + task.getException());
//                }
//            }
//        });
//    }

    private void refreshRoutes() {
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> myData = documentSnapshot.getData();
                    System.out.println(myData.get("route"));
                    //TODO create json object and extract all the strings, especially the image URIs and then add them to the frame list
                    
//                    routeNameList.add((String) myData.get("route"));
//                    System.out.println(routeNameList.get(0));
                }
            }
        });
    }

}