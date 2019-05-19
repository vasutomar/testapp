package com.example.vasu.redcarpet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.skyfishjy.library.RippleBackground;

public class MainActivity extends AppCompatActivity {

    private static String[] permissions = {Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        rippleBackground.startRippleAnimation();

        FirebaseApp.initializeApp(MainActivity.this);
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("redcarpet-6a2fb") // Required for Analytics.
                .setApiKey("AIzaSyDpaUk1r68Jw2OJ_WR4yU0peq7K8aOIIZ8") // Required for Auth.
                .setDatabaseUrl("https://redcarpet-6a2fb.firebaseio.com/") // Required for RTDB.
                // ...
                .build();
        FirebaseApp.initializeApp(this /* Context */, options, "secondary");

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this,EntryActivity.class);
                startActivity(intent);
            }
        }, 3000);

    }



}
