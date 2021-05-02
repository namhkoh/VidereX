package com.kohdev.viderex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.ls.LSOutput;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.grpc.internal.JsonUtil;

public class RouteListViewActivity extends AppCompatActivity {

    private ArrayAdapter adapter;
    private ListView routeOptions;

    String json;
    Map jsonMap;
    String routeName;
    String privateId;
    ArrayList<Uri> framePathList = new ArrayList<Uri>();
    ArrayList<Uri> image_path = new ArrayList<Uri>();
    ArrayList<Uri> view_path = new ArrayList<Uri>();
    ArrayList<String> routeNameList = new ArrayList<String>();
    ArrayList<String> privateIds = new ArrayList<String>();

    Map<String, Object> finalMap = new HashMap<String, Object>();
    Map<String, Object> keyMap = new HashMap<String, Object>();


    JSONObject currentRoute;
    JSONObject selectedObj;

    JSONObject obj;
    JSONObject routeObj;

    private DocumentReference mDocRef = FirebaseFirestore.getInstance().document("RouteObject/C3Mld3o8fOLPaFQttnm5");
    private CollectionReference mCollRef = FirebaseFirestore.getInstance().collection("RouteObject");
    Source source = Source.CACHE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list_view);
        loadCollection();
//        Button refresh = findViewById(R.id.refresh);
//        refresh.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                refreshRoutes();
//            }
//        });
//        getRouteName("WCBceTXobYoLINMaJw2G", "table");
        try {
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
        } catch (NullPointerException n) {
            n.printStackTrace();
        }

        //refreshRoutes();


        //Used to automatically update the list on the screen.
//        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, routeNameList) {
//            @NonNull
//            @Override
//            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//                TextView item = (TextView) super.getView(position, convertView, parent);
//                item.setTextColor(Color.parseColor("white"));
//                item.setTypeface(item.getTypeface(), Typeface.BOLD);
//                item.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
//                item.setAlpha(0.7f);
//                return item;
//            }
//        };
//        routeOptions = findViewById(R.id.route_list);
//        routeOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String item = (String) adapter.getItem(position);
//                System.out.println(item);
//                for (int i = 0; i < routeOptions.getChildCount(); i++) {
//                    if (position == i) {
//                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#8c8c8c"));
//                    } else {
//                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#f2f2f2"));
//                    }
//                }
//
//                // Find the correct route with name
//                if (item.equals(routeName)) {
//                    Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
//                    intent.putExtra("route_json", json);
//                    intent.putExtra("image_path", framePathList);
//                    startActivity(intent);
//                }
//            }
//        });
//        routeOptions.setAdapter(adapter);
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

    /**
     * This method will access the route by privateID and get the routeName of the snapshot
     * For the privateID, get the routeName, if that routeName matches the one pressed by the user, then access that specific jsonObject.
     *
     * @return String routeName
     */
    private ArrayList<Uri> getRouteName(String privateId, final String selectedRoute) {
        System.out.println("privateId: " + privateId);
        System.out.println("selectedRoute: " + selectedRoute);
        DocumentReference mDocRef = FirebaseFirestore.getInstance().document("RouteObject/" + privateId);
        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> myData = documentSnapshot.getData();
                    try {
                        selectedObj = new JSONObject((String) myData.get("route"));
//                        routeName = selectedObj.getString("name");
//                        System.out.println(routeName);
//                        System.out.println(selectedRoute);
                        JSONArray snap = selectedObj.getJSONArray("snapshots");
                        System.out.println(snap);
                        for (int i = 0; i < snap.length(); i++) {
                            JSONObject snapObj = snap.getJSONObject(i);
                            Uri imageUri = Uri.parse(snapObj.getString("preprocessed_img_uri"));
                            view_path.add(imageUri);
                            Collections.sort(view_path);
                            System.out.println(view_path);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mDocRef.get().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("ERROR ", String.valueOf(e));
            }
        });
        return view_path;
    }

    private void loadCollection() {
        mCollRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Map<String, Object> myData = documentSnapshot.getData();
                    privateId = documentSnapshot.getId();
                    try {
                        routeObj = new JSONObject((String) myData.get("route"));
                        routeName = routeObj.getString("name");
                        routeNameList.add(routeName);
                        finalMap.put(routeName, routeObj);
                        keyMap.put(routeName, privateId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, routeNameList) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        TextView item = (TextView) super.getView(position, convertView, parent);
                        //item.setTextColor(Color.parseColor("#000000"));
                        item.setTextColor(Color.WHITE);
                        item.setTypeface(item.getTypeface(), Typeface.BOLD);
                        item.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
                        item.setAlpha(0.7f);
                        return item;
                    }
                };

                routeOptions = findViewById(R.id.route_list);
                routeOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final String item = (String) adapter.getItem(position);
                        System.out.println(item);
                        for (int i = 0; i < routeOptions.getChildCount(); i++) {
                            if (position == i) {
                                routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#8c8c8c"));
                            }
//                            else {
////                                routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#f2f2f2"));
//                                routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#f2f2f2"));
//                            }
                        }

                        for (String k : finalMap.keySet()) {
                            if (item.equals(k)) {
                                image_path = getRouteName((String) keyMap.get(k), item);
                                System.out.println("NAMHO");
                                System.out.println(image_path);
                            }
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("accessed");
                                Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
                                intent.putExtra("route_json", json);
                                intent.putExtra("image_path", image_path);
                                startActivity(intent);
                            }
                        }, 3000);

//                                Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
//                                intent.putExtra("route_json", json);
//                                intent.putExtra("image_path", image_path);
//                                startActivity(intent);

                    }
                });
                routeOptions.setAdapter(adapter);
            }
        });
    }

//    private void loadCollection_() {
//        mCollRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//            @Override
//            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                    Map<String, Object> myData = documentSnapshot.getData();
//                    privateId = documentSnapshot.getId();
//                    try {
//                        routeObj = new JSONObject((String) myData.get("route"));
//                        System.out.println(routeObj);
//                        routeName = routeObj.getString("name");
//                        routeNameList.add(routeName);
//                        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, routeNameList) {
//                            @NonNull
//                            @Override
//                            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//                                TextView item = (TextView) super.getView(position, convertView, parent);
//                                item.setTextColor(Color.parseColor("#000000"));
//                                item.setTypeface(item.getTypeface(), Typeface.BOLD);
//                                item.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
//                                item.setAlpha(0.7f);
//                                return item;
//                            }
//                        };
//
//                        routeOptions = findViewById(R.id.route_list);
//                        routeOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                            @Override
//                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                                String item = (String) adapter.getItem(position);
//                                System.out.println(item);
//                                for (int i = 0; i < routeOptions.getChildCount(); i++) {
//                                    if (position == i) {
//                                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#8c8c8c"));
//                                        System.out.println("action 1");
//                                    } else {
//                                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#f2f2f2"));
//                                        System.out.println("action 2");
//                                    }
//                                }
//
////                                if (routeName.equals(item)) {
////                                    image_path = getRouteName(privateId, item);
////                                    Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
////                                    intent.putExtra("route_json", json);
////                                    intent.putExtra("image_path", image_path);
////                                    startActivity(intent);
////                                }
//
//                            }
//                        });
//                        routeOptions.setAdapter(adapter);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//    }

//    //TODO create json object and extract all the strings, especially the image URIs and then add them to the frame list
//    private void refreshRoutes() {
//        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//            @Override
//            public void onSuccess(DocumentSnapshot documentSnapshot) {
//                if (documentSnapshot.exists()) {
//                    Map<String, Object> myData = documentSnapshot.getData();
//                    try {
//                        obj = new JSONObject((String) myData.get("route"));
//                        routeName = obj.getString("name");
//                        routeNameList.add(routeName);
//                        JSONArray snap = obj.getJSONArray("snapshots");
//                        for (int i = 0; i < snap.length(); i++) {
//                            JSONObject snapObj = snap.getJSONObject(i);
//                            Uri imageUri = Uri.parse(snapObj.getString("preprocessed_img_uri"));
//                            image_path.add(imageUri);
//                            Collections.sort(image_path);
//                            System.out.println(image_path);
//                            System.out.println(imageUri);
//                        }
//                        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, routeNameList) {
//                            @NonNull
//                            @Override
//                            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//                                TextView item = (TextView) super.getView(position, convertView, parent);
//                                item.setTextColor(Color.parseColor("#000000"));
//                                item.setTypeface(item.getTypeface(), Typeface.BOLD);
//                                item.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
//                                item.setAlpha(0.7f);
//                                return item;
//                            }
//                        };
//                        routeOptions = findViewById(R.id.route_list);
//                        routeOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                            @Override
//                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                                String item = (String) adapter.getItem(position);
//                                System.out.println(item);
//                                for (int i = 0; i < routeOptions.getChildCount(); i++) {
//                                    if (position == i) {
//                                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#8c8c8c"));
//                                    } else {
//                                        routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#f2f2f2"));
//                                    }
//                                }
//
//                                // Find the correct route with name
//                                if (item.equals(routeName)) {
//                                    Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
//                                    intent.putExtra("route_json", json);
//                                    intent.putExtra("image_path", image_path);
//                                    startActivity(intent);
//                                }
//                            }
//                        });
//                        routeOptions.setAdapter(adapter);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//    }


}