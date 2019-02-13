package com.tg.lucas.apptg;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MainActivity extends WearableActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "mobile/" + MainActivity.class.getSimpleName() + "/";
    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private static final int LOCATION_REQUEST_CODE = 102;
    private MapXmlParser parser;
    private CoordinatesDbHelper dbHelper;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        parser = new MapXmlParser(getApplicationContext());
        createDatabase();
        getLastLocation();
        verifyPermissions();
    }

    private void createDatabase() {
        Log.d(TAG, "createDatabase: init");
        dbHelper = new CoordinatesDbHelper(getApplicationContext());
        // Cria o banco de dados
        dbHelper.getWritableDatabase();
    }

    private void getLastLocation() {
        if (isOnline()) {
            checkLocationPermission();
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.d(TAG, "onSuccess: "+location);
                }
            }).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    checkLocationPermission();
                    fusedLocationClient.getLastLocation();
                }
            });
        }
    }

    private void checkLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
        }
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if(connMgr != null){
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void verifyPermissions() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        // Explicar para o usuário o motivo da permissão de maneira ASSÍNCRONA
        //
        //--------------------------------------------------------------------
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            //TODO: descomentar o parseXml()
            //parseXml();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_REQUEST_CODE:
                //TODO: descomentar o parseXml()
                //parseXml();
                break;
            case LOCATION_REQUEST_CODE:
                break;
        }
    }

    private void parseXml() {
        Log.d(TAG, "xmlParse: " + Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/mapaSorocaba.xml");
        String pathname = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/mapaSorocaba.xml";
        File file = new File(pathname);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            parser.parse(fileInputStream);
            parseBounds();
            insertNodes();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    private void parseBounds() {
        MapXmlParser.Bounds bounds = parser.getNewBounds();
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(getApplicationContext());
        dbHelper.insertBounds(dbHelper.getWritableDatabase(), bounds);
    }

    private void insertNodes() {
        List nodeLatLng = parser.getNodeLatLng();
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(getApplicationContext());
        for(int i = 0; i< nodeLatLng.size(); i++){
            MapXmlParser.Entry node = (MapXmlParser.Entry) nodeLatLng.get(i);
            double _nodeId = node.node_id;
            double _nodeLat = node.node_lat;
            double _nodeLng = node.node_lng;
            dbHelper.insertOrReplace(dbHelper.getWritableDatabase(), _nodeId, _nodeLat, _nodeLng);
        }
        dbHelper.close();
    }
}
