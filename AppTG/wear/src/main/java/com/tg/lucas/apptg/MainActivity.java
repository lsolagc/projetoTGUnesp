package com.tg.lucas.apptg;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends WearableActivity {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        createDatabase();
    }

    private void createDatabase() {
        Log.d(TAG, "createDatabase: init");
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(getApplicationContext());

        // Cria o banco de dados
        dbHelper.getWritableDatabase();
    }
}
