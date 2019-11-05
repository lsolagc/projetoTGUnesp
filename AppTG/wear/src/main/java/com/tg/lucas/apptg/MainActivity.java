package com.tg.lucas.apptg;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends WearableActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        View.OnClickListener {

    private static final String TAG = "mobile/" + MainActivity.class.getSimpleName() + "/";
    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private static final int LOCATION_REQUEST_CODE = 102;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 400;
    private MapXmlParser parser;
    private CoordinatesDbHelper dbHelper;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private OutputStream outputStream;
    private float maxLat;
    private float maxLng;
    private float minLat;
    private float minLng;
    private Location location;
    private TextToSpeech TTS;
    private String falar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();


        verifyPermissions();
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            // Create the LocationRequest object
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                    .setFastestInterval(1 * 1000); // 1 second, in milliseconds

            mGoogleApiClient.connect();
            if(mGoogleApiClient.isConnecting()){
                Log.d(TAG, "Wait for connection");
            }
            else if(mGoogleApiClient.isConnected()){
                Log.d(TAG, "Google API Client CONNECTED");
                getLastLocation();

            }
            else if(!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }
        }
        else{
            verifyPermissions();
        }

        parser = new MapXmlParser(getApplicationContext());
        createDatabase();
        setupTTS();

        DownloadAndReadMap();

        Button btn = findViewById(R.id.button_test);
        btn.setOnClickListener(this);

    }

    private void setupTTS() {
        TTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if(status == TextToSpeech.SUCCESS){
                    int PT_BR_Available = TTS.isLanguageAvailable(new Locale("pt", "BR"));
                    if(PT_BR_Available == TextToSpeech.LANG_COUNTRY_AVAILABLE){
                        TTS.setLanguage(Locale.getDefault());
                    }
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mGoogleApiClient.isConnected()){
            mGoogleApiClient.connect();
        }
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
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
            else {
                handleNewLocation(location);
            }
        }
    }

    private void handleNewLocation(Location _location) {
        Log.d(TAG, _location.toString());
        location = _location;
        maxLat = (float) _location.getLatitude() + (float)0.05;
        minLat = (float) _location.getLatitude() - (float)0.05;
        maxLng = (float) _location.getLongitude() + (float)0.1;
        minLng = (float) _location.getLongitude() - (float)0.1;
    }

    public void falarEndereco(){
        Log.d(TAG, "btn_click: CLICK");
        try {
            String way = dbHelper.getNearestLocation(dbHelper.getWritableDatabase(), location);
//            MediaPlayer mp = MediaPlayer.create(this, R.raw.test);
//            mp.start();
            falar = "Você está em " + way;
            TTS.speak(falar, TextToSpeech.QUEUE_FLUSH, null);
        }
        catch(Exception e){
            Log.d(TAG, "btn_click: error:", e);
        }
    }

    private void DownloadAndReadMap() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    try {
                        falar = "Baixando mapa da área";
                        TTS.speak(falar, TextToSpeech.QUEUE_ADD, null);

                        String downloadLink = "https://api.openstreetmap.org/api/0.6/map?bbox="
                                +minLng+","+minLat+","+maxLng+","+maxLat+"";
                        URL url = new URL(downloadLink);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");
                        int status = con.getResponseCode();
                        Log.d(TAG, "run: status"+ status);

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }

                        try {
                            String text = content.toString();
                            outputStream = openFileOutput("MapFile.txt", MODE_PRIVATE);
                            outputStream.write(text.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally {
                            try {
                                outputStream.close();
                            }
                            catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                        File initialFile = new File("/data/data/com.tg.lucas.apptg/files/MapFile.txt");
                        InputStream stream = new FileInputStream(initialFile);

                        falar = "Inserindo endereços no banco de dados";
                        TTS.speak(falar, TextToSpeech.QUEUE_ADD, null);

                        parseXml(stream);
                    }
                    catch (Exception e){
                        Log.d(TAG, "doInBackground: " + e.getMessage(), e);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    private void checkLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
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
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION};
        // Explicar para o usuário o motivo da permissão de maneira ASSÍNCRONA
        //
        //--------------------------------------------------------------------
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, EXTERNAL_STORAGE_REQUEST_CODE);
        } else if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
        }
        else{
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

    private void parseXml(InputStream inputStream) {
        try {
            parser.parse(inputStream);
            parseBounds();
            insertNodes();
            insertWayNames();
            Log.d(TAG, "parseXml: fim do parse");
            falar = "Banco de dados pronto. Toque na tela para ouvir o nome da rua onde você está agora.";
            TTS.speak(falar, TextToSpeech.QUEUE_ADD, null);
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
        Log.d(TAG, "insertNodes: inserindo Nodes");
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

    private void insertWayNames(){
        List ways  = parser.getNodeWay();
        Log.d(TAG, "insertWayNames: inserindo WayNames");
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(getApplicationContext());
        for(int i = 0; i< ways.size(); i++){
            List<MapXmlParser.Entry> node = (List<MapXmlParser.Entry>) ways.get(i);
            for(int j = 0; j < node.size(); j++){
                String tableName = CoordinatesContract.CoordinatesEntry.TABLE_NAME_COORDINATES;
                String columnNodeId = CoordinatesContract.CoordinatesEntry.COLUMN_NODE_ID;
                String columnWayName = CoordinatesContract.CoordinatesEntry.COLUMN_WAY_NAME;
                ContentValues cv = new ContentValues();
                cv.put(columnWayName, node.get(j).way_name);
                String[] args = new String[]{Double.toString(node.get(j).node_id)};
                dbHelper.getWritableDatabase().update(tableName,
                        cv,
                        "" + columnNodeId + " = ?",
                        args);
                }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onClick(android.view.View v) {
        falarEndereco();
    }
}
