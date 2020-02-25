package com.tg.lucas.apptg;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Location;
import android.location.Geocoder;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

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
//    private OutputStream outputStream;
    private Location location;
    private TextToSpeech TTS;
    private String falar;
    private boolean offlineMapReady = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        setupTTS();
        verifyPermissions();
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(10 * 1000)        // 10 segundos, em milissegundos
                    .setFastestInterval(1 * 1000); // 1 segundo, em milissegundos

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


//        DownloadAndReadMap();

        // getWayNames();

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
    }

    public void falarEndereco(){
        Log.d(TAG, "btn_click: CLICK");
        String way = "null";
        try {
            way = dbHelper.getNearestLocation(dbHelper.getWritableDatabase(), location);
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
                            File initialFile = new File("/data/data/com.tg.lucas.apptg/files/sorocaba.xml");
                            InputStream stream = new FileInputStream(initialFile);

                            falar = "Inserindo endereços no banco de dados";
                            TTS.speak(falar, TextToSpeech.QUEUE_ADD, null);

                            parseXml(stream);
                            offlineMapReady = true;
//                        }
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
                PackageManager.PERMISSION_GRANTED)
        {
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
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION};
        falar = "Este aplicativo precisa utilizar o GPS do relógio para funcionar corretamente. " +
                "Por favor, forneça as permissões necessárias ao tocar duas vezes no canto inferior" +
                "direito da tela, com alguns segundos de intervalo.";
        TTS.speak(falar, TextToSpeech.QUEUE_FLUSH, null);
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
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
//            insertWayNames();
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
//            dbHelper.insertOrReplace(dbHelper.getWritableDatabase(), _nodeId, _nodeLat, _nodeLng);
            dbHelper.insertOrReplace(dbHelper.getWritableDatabase(), _nodeLat, _nodeLng);
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

    public void getWayNames(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor cursor = db.query(CoordinatesContract.CoordinatesEntry.TABLE_NAME_COORDINATES,
                            new String[]{"*"},
                            "",
                            null,
                            null,
                            null,
                            ""
                    );
                    while(cursor.moveToNext()){
                        try{
                            Geocoder geocoder = new Geocoder(getApplicationContext());
                            double lat = Double.parseDouble(cursor.getString(1));
                            double lng = Double.parseDouble(cursor.getString(2));
                            List<Address> addresses = geocoder.getFromLocation(lat,lng,1);
                            String addr = addresses.get(0).getThoroughfare() + ", " + addresses.get(0).getSubThoroughfare();

                            String sqlQuery = "UPDATE " + CoordinatesContract.CoordinatesEntry.TABLE_NAME_COORDINATES +
                                    " SET " + CoordinatesContract.CoordinatesEntry.COLUMN_WAY_NAME + "=\"" + addr + "\"" +
                                    " WHERE _id=" + cursor.getString(0) + ";";
                            db.execSQL(sqlQuery);

                            String downloadLink = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                                    cursor.getString(1) + "," +
                                    cursor.getString(2) +
                                    "&type=establishment&radius=1000&key=" + getString(R.string.API_KEY) + "";

                            URL url = new URL(downloadLink);
                            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                            con.setReadTimeout(15000);
                            con.setConnectTimeout(15000);
                            con.setRequestMethod("GET");
                            con.setDoInput(true);
                            con.setRequestProperty("Connection", "Keep-Alive");
                            con.setRequestProperty("Content-Type",
                                    "application/x-www-form-urlencoded");
                            con.setDoOutput(true);
                            int status = con.getResponseCode();
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(con.getInputStream()));
                            String inputLine;
                            StringBuffer content = new StringBuffer();
                            while ((inputLine = in.readLine()) != null) {
                                content.append(inputLine);
                            }

                            StringBuilder placesNames = new StringBuilder();
                            JSONObject json = new JSONObject(content.toString());
                            JSONArray nearbyPlaces = json.getJSONArray("results");
                            int regPlaces = 0;

                            for(int i=0; i< nearbyPlaces.length(); i++){
                                JSONObject jsonObject = nearbyPlaces.getJSONObject(i);
                                String t = jsonObject.getString("types");
                                t = t.substring(1, t.length() -1);
                                String[] types = t.split(",");
                                List<String> list = Arrays.asList(types);

                                if(list.contains("\"establishment\"")) {
                                    regPlaces++;
                                    placesNames.append(jsonObject.getString("name"));
                                    if(regPlaces < 3) {
                                        placesNames.append(", ");
                                    }
                                    else{
                                        break;
                                    }
                                }

                            }

                            ContentValues cv = new ContentValues();
                            cv.put(CoordinatesContract.CoordinatesEntry.COLUMN_NEAR_PLACES, placesNames.toString());
                            db.update(CoordinatesContract.CoordinatesEntry.TABLE_NAME_COORDINATES, cv, "_id="+cursor.getString(0), null);
                            con.disconnect();
                            Log.d(TAG, "run: " + cursor.getString(0));
                        }catch (IOException | JSONException e ){
                            e.printStackTrace();
                        }



                    }
                    cursor.close();
                    Log.d(TAG, "run: ");
            }
        }).start();
    }
}
