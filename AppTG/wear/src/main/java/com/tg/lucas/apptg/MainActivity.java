package com.tg.lucas.apptg;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends WearableActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 101 ;
    private final String TAG = this.getClass().getSimpleName();
    private MapXmlParser parser = new MapXmlParser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        createDatabase();
        verifyPermissions();
    }

    private void createDatabase() {
        Log.d(TAG, "createDatabase: init");
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(getApplicationContext());

        // Cria o banco de dados
        dbHelper.getWritableDatabase();
    }

    private void verifyPermissions(){
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
        // No explanation needed, we can request the permission.
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, permissions, EXTERNAL_STORAGE_REQUEST_CODE);
        }
        else{
            parseXml();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case EXTERNAL_STORAGE_REQUEST_CODE:
                parseXml();
                break;
        }
    }

    private void parseXml() {
        Log.d(TAG, "xmlParse: "+Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS+"/mapaSorocaba.xml");
        String pathname = Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS+"/mapaSorocaba.xml";
        File file = new File(pathname);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            parser.parse(fileInputStream);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}
