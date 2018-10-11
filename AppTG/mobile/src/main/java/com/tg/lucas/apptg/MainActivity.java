package com.tg.lucas.apptg;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "mobile/" + MainActivity.class.getSimpleName() + "/";
    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private MapXmlParser parser = new MapXmlParser();
    private CoordinatesDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        createDatabase();
        verifyPermissions();
    }

    private void createDatabase() {
        Log.d(TAG, "createDatabase: init");
        dbHelper = new CoordinatesDbHelper(getApplicationContext());
        // Cria o banco de dados
        dbHelper.getWritableDatabase();
    }

    private void verifyPermissions() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        // Explicar para o usuário o motivo da permissão de maneira ASSÍNCRONA
        //
        //--------------------------------------------------------------------
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            parseXml();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_REQUEST_CODE:
                parseXml();
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
            insertBounds();
            insertNodes();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    private void insertBounds() {
        MapXmlParser.Bounds bounds = parser.getBounds();
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(getApplicationContext());
        dbHelper.verifyBounds(dbHelper.getWritableDatabase(), bounds);
    }

    private void insertNodes() {
        List nodeLatLng = parser.getNodeLatLng();
        //Log.d(TAG, "insertNodes: nodeLatLng.size = "+nodeLatLng.size());
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(getApplicationContext());
        for(int i = 0; i< nodeLatLng.size(); i++){
            MapXmlParser.Entry node = (MapXmlParser.Entry) nodeLatLng.get(i);
            double _nodeId = node.node_id;
            double _nodeLat = node.node_lat;
            double _nodeLng = node.node_lng;
            dbHelper.insertOrReplace(dbHelper.getWritableDatabase(), _nodeId, _nodeLat, _nodeLng);
            //Log.d(TAG, "insertNodes: node inserido com sucesso! "+i);
        }
        dbHelper.close();
        //Log.d(TAG, "insertNodes: TODOS OS NODES FORAM INSERIDOS COM SUCESSO");
    }

}
