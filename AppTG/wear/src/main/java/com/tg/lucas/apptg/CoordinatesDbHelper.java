package com.tg.lucas.apptg;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

public class CoordinatesDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "TG.db";
    private static final String TAG = "mobile/" + CoordinatesDbHelper.class.getSimpleName() + "/";

    public CoordinatesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CoordinatesContract.CoordinatesEntry.SQL_CREATE_TABLE_COORDINATES);
        db.execSQL(CoordinatesContract.CoordinatesEntry.SQL_CREATE_TABLE_PLACES);
        db.execSQL(CoordinatesContract.CoordinatesEntry.SQL_CREATE_TABLE_BOUNDS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(CoordinatesContract.CoordinatesEntry.SQL_DELETE_TABLE_COORDINATES);
        db.execSQL(CoordinatesContract.CoordinatesEntry.SQL_DELETE_TABLE_BOUNDS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

//    public void insertOrReplace(SQLiteDatabase db, double _nodeId, double _nodeLat, double _nodelng) {
    public void insertOrReplace(SQLiteDatabase db, double _nodeLat, double _nodelng) {
        String tableName = CoordinatesContract.CoordinatesEntry.TABLE_NAME_COORDINATES;
        String columnNodeLat = CoordinatesContract.CoordinatesEntry.COLUMN_NODE_LAT;
        String columnNodeLng = CoordinatesContract.CoordinatesEntry.COLUMN_NODE_LNG;
        String sqlQuery = "INSERT OR REPLACE INTO " + tableName +
                " (" + columnNodeLat + ", " + columnNodeLng + ") " +
                "VALUES (  " +
                  _nodeLat + ", " + _nodelng + " " +
                ");";
        db.execSQL(sqlQuery);
    }

    public void insertBounds(SQLiteDatabase db, MapXmlParser.Bounds bounds) {
        String tableName = CoordinatesContract.CoordinatesEntry.TABLE_NAME_BOUNDS;
        String columnMaxLat = CoordinatesContract.CoordinatesEntry.COLUMN_MAX_LAT;
        String columnMinLat = CoordinatesContract.CoordinatesEntry.COLUMN_MIN_LAT;
        String columnMaxLon = CoordinatesContract.CoordinatesEntry.COLUMN_MAX_LNG;
        String columnMinLon = CoordinatesContract.CoordinatesEntry.COLUMN_MIN_LNG;
        String sqlQuery;

        String sqlSelectQuery = "SELECT * FROM " + tableName + ";";

        Cursor cursor = db.rawQuery(sqlSelectQuery, new String[]{});
        if (cursor.moveToFirst()) {
            Log.d(TAG, "insertBounds: existe resposta no SELECT");
            sqlQuery = "UPDATE " + tableName +
                    " SET " + columnMaxLat + "=" + bounds._maxLat + ", " +
                    columnMinLat + "=" + bounds._minLat + ", " +
                    columnMaxLon + "=" + bounds._maxLng + ", " +
                    columnMinLon + "=" + bounds._minLng + ";";
        } else {
            Log.d(TAG, "insertBounds: não existem entradas na tabela bounds");
            sqlQuery = "INSERT INTO " + tableName +
                    " (" + columnMaxLat + ", " + columnMinLat + ", " + columnMaxLon + ", " + columnMinLon + ") " +
                    "VALUES (  " +
                    bounds._maxLat + ", " +
                    bounds._minLat + ", " +
                    bounds._maxLng + ", " +
                    bounds._minLng +
                    ");";
        }
        db.execSQL(sqlQuery);
        cursor.close();
    }

    public MapXmlParser.Bounds getBounds(SQLiteDatabase db){
        String tableName = CoordinatesContract.CoordinatesEntry.TABLE_NAME_BOUNDS;
        MapXmlParser.Bounds bounds = null;

        String sqlSelectQuery = "SELECT * FROM " + tableName + ";";

        Cursor cursor = db.rawQuery(sqlSelectQuery, new String[]{});
        if(cursor.moveToFirst()){
            double _maxLat = cursor.getDouble(1);
            double _minLat = cursor.getDouble(2);
            double _maxLng = cursor.getDouble(3);
            double _minLng = cursor.getDouble(4);
            bounds = new MapXmlParser.Bounds(_maxLat, _maxLng, _minLat, _minLng);
        }
        cursor.close();
        return bounds;
    }

    public String getNearestLocation(SQLiteDatabase db, Location location){
        String tableName = CoordinatesContract.CoordinatesEntry.TABLE_NAME_COORDINATES;
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();
        Cursor cursor = db.query(tableName,
                new String[]{"*"},
                "ABS(" + lng + " - node_lng) <= ABS(" + lat + " - node_lat)",
                null,
                null,
                null,
                " ABS(" + lat + " - node_lat)",
                String.valueOf(1)
                );
        cursor.moveToFirst();
        String way = cursor.getString(cursor.getColumnIndex("way_name"));
        String places = cursor.getString(cursor.getColumnIndex("near_places"));
        Log.d(TAG, "getNearestLocation: test");
        cursor.close();
        String result = "" + way + ";" + places + "";
        return result;
    }

}
