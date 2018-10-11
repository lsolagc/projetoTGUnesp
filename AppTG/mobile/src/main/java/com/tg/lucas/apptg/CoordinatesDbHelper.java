package com.tg.lucas.apptg;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

    public void insertOrReplace(SQLiteDatabase db, double _nodeId, double _nodeLat, double _nodelng) {
        String tableName = CoordinatesContract.CoordinatesEntry.TABLE_NAME_COORDINATES;
        String columnNodeId = CoordinatesContract.CoordinatesEntry.COLUMN_NODE_ID;
        String columnNodeLat = CoordinatesContract.CoordinatesEntry.COLUMN_NODE_LAT;
        String columnNodeLng = CoordinatesContract.CoordinatesEntry.COLUMN_NODE_LNG;
        String sqlQuery = "INSERT OR REPLACE INTO " + tableName +
                " (" + columnNodeId + ", " + columnNodeLat + ", " + columnNodeLng + ") " +
                "VALUES (  " +
                _nodeId + ", " +
                "COALESCE((SELECT "+columnNodeLat+" FROM "+tableName+" WHERE "+columnNodeId+"="+_nodeId+"),"+_nodeLat + "), " +
                "COALESCE((SELECT "+columnNodeLng+" FROM "+tableName+" WHERE "+columnNodeId+"="+_nodeId+"),"+_nodelng + ")" +
                ");";
        db.execSQL(sqlQuery);
    }

    public void verifyBounds(SQLiteDatabase db, MapXmlParser.Bounds bounds) {
        String tableName = CoordinatesContract.CoordinatesEntry.TABLE_NAME_BOUNDS;
        String columnMaxLat = CoordinatesContract.CoordinatesEntry.COLUMN_MAX_LAT;
        String columnMinLat = CoordinatesContract.CoordinatesEntry.COLUMN_MIN_LAT;
        String columnMaxLon = CoordinatesContract.CoordinatesEntry.COLUMN_MAX_LNG;
        String columnMinLon = CoordinatesContract.CoordinatesEntry.COLUMN_MIN_LNG;
        String sqlQuery = null;

        String sqlSelectQuery = "SELECT * FROM "+tableName+";";

        Cursor cursor = db.rawQuery(sqlSelectQuery, new String[]{});
        if(cursor.moveToFirst()){
            Log.d(TAG, "verifyBounds: existe resposta no SELECT");
            sqlQuery = "UPDATE " + tableName +
                    " SET '"+ columnMaxLat + "'="+bounds._maxLat+", '" + columnMinLat + "', '" + columnMaxLon + "', '"+columnMinLon+"' " +
                    "WHERE '_id' = 1;";
        }
        else{
            Log.d(TAG, "verifyBounds: não existem entradas na tabela bounds");
            sqlQuery = "INSERT INTO " + tableName +
                    " (" + columnMaxLat + ", " + columnMinLat + ", " + columnMaxLon + ", "+columnMinLon+") " +
                    "VALUES (  " +
                    bounds._maxLat + ", " +
                    bounds._minLat + ", " +
                    bounds._maxLng + ", " +
                    bounds._minLng +
                    ");";
        }
        db.execSQL(sqlQuery);
        /*

        */
    }
}
