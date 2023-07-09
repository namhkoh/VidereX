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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSOutput;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.grpc.internal.JsonUtil;

public class RouteListViewActivity extends AppCompatActivity {

    private ArrayAdapter adapter;
    private ListView routeOptions;

    String json;
    String routeName;
    String privateId;
    ArrayList<Uri> framePathList = new ArrayList<Uri>();
    ArrayList<Uri> image_path = new ArrayList<Uri>();
    ArrayList<Uri> view_path = new ArrayList<Uri>();
    ArrayList<String> routeNameList = new ArrayList<String>();

    Map<String, Object> finalMap = new HashMap<String, Object>();
    Map<String, Object> keyMap = new HashMap<String, Object>();


    JSONObject selectedObj;

    JSONObject routeObj;

    private DocumentReference mDocRef = FirebaseFirestore.getInstance().document("RouteObject/C3Mld3o8fOLPaFQttnm5");
    private CollectionReference mCollRef = FirebaseFirestore.getInstance().collection("RouteObject");
    //Android speech
    private static final String TAG = "SPEECH";
    private TextToSpeech textToSpeech;
    private ImageButton speechButton;
    String userInput = "";
    boolean good_match = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list_view);
        Log.d("inner_layer","entering route list");
        loadCollection();

        speechButton = findViewById(R.id.speechButton);

        try {
            framePathList = (ArrayList<Uri>) getIntent().getSerializableExtra("uriList");
            json = (String) getIntent().getSerializableExtra("route_json");
            //routeNameList.add(json);
            System.out.println(framePathList);
            System.out.println(json);
            Log.d("inner_layer",framePathList.get(0).toString());
            Log.d("inner_layer",json);
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

        final SpeechRecognizer mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.d(TAG, "onReadyForSpeech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningfSpeech");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                Log.e(TAG, "this is on end of speech.");
            }

            @Override
            public void onError(int i) {
                Log.e(TAG, "on Error: " + i);

            }

            @Override
            public void onResults(Bundle bundle) {
                Log.d(TAG, "on Results");
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                userInput = matches.get(0);
                //utterance.setText("Detected utterance: " + userInput);
                Log.d("inner_layer_userInput", userInput);

                mCollRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Map<String, Object> myData = documentSnapshot.getData();
                        privateId = documentSnapshot.getId();
                        Log.d("inner_layer_privateId",privateId);
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

                    for (String k : finalMap.keySet()) {
                        System.out.println(("sample_route").contains("sample"));
                        if (k.contains(userInput)) {
                            good_match = true;
                            initTTS("Starting " + k + " for navigation.");
                            System.out.println(userInput);
                            image_path = getRouteName((String) keyMap.get(k), k);
                        } else {
                            good_match = false;
                        }
                    }
                    if (good_match = false) {
                        initTTS("Please repeat the route name.");
                    } else if (good_match = true) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
                            intent.putExtra("route_json", json);
                            intent.putExtra("image_path", image_path);
                            startActivity(intent);
                        }, 3000);
                    }
                });

            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        speechButton.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    mSpeechRecognizer.stopListening();
//                    stopRecognition();
//                    stopRecording();
                    break;

                case MotionEvent.ACTION_DOWN:
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                    System.out.println("listening");
//                    startRecording();
//                    startRecognition();

                    break;
            }
            return false;
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {

        });

        //Results of pressing the speech button.
        textToSpeech.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        speechButton.setEnabled(false);
                    }

                    @Override
                    public void onDone(String utteranceId) {

                    }

                    @Override
                    public void onError(String utteranceId) {

                    }
                });


    }

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
        Log.d("inner_layer","loadCollection called.");
        mCollRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Map<String, Object> myData = documentSnapshot.getData();
                    privateId = documentSnapshot.getId();
                    try {
                        routeObj = new JSONObject((String) Objects.requireNonNull(myData.get("route")));
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
                        Log.d("inner_layer","adapter_called");
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
                        Log.d("inner_layer_item",item);
                        for (int i = 0; i < routeOptions.getChildCount(); i++) {
                            if (position == i) {
                                routeOptions.getChildAt(i).setBackgroundColor(Color.parseColor("#8c8c8c"));
                            }
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
                                System.out.println("intermeditate");
                                Intent intent = new Intent(getApplicationContext(), DebugViewActivity.class);
                                intent.putExtra("route_json", json);
                                intent.putExtra("image_path", image_path);
                                startActivity(intent);
                            }
                        }, 3000);
                    }
                });
                routeOptions.setAdapter(adapter);
            }
        });
    }

    /**
     * A voice reads the text given in the method.
     *
     * @param selectedText The String text that is read.
     */
    private void initTTS(String selectedText) {
        //textToSpeech.setSpeechRate(testingVal);
        int speechStatus = textToSpeech.speak(selectedText, TextToSpeech.QUEUE_ADD, null, "1");
        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }


}