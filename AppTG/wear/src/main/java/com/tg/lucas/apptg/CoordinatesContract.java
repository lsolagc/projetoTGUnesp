package com.tg.lucas.apptg;

import android.provider.BaseColumns;

public final class CoordinatesContract {

    private CoordinatesContract(){

    }

    static class CoordinatesEntry implements BaseColumns{
        static final String TABLE_NAME_COORDINATES = "coordinates";
        static final String TABLE_NAME_PLACES = "places";
        static final String TABLE_NAME_BOUNDS = "bounds";
        static final String COLUMN_NODE_ID = "node_id";
        static final String COLUMN_PLACE_ID = "place_id";
        static final String COLUMN_NODE_LAT = "node_lat";
        static final String COLUMN_NODE_LNG = "node_lng";
        static final String COLUMN_WAY_NAME = "way_name";
        static final String COLUMN_PLACE_NAME = "place_name";
        static final String COLUMN_NEAR_PLACES = "near_places";
        static final String COLUMN_MAX_LAT = "max_lat";
        static final String COLUMN_MIN_LAT = "min_lat";
        static final String COLUMN_MAX_LNG = "max_lng";
        static final String COLUMN_MIN_LNG = "min_lng";

        static final String SQL_CREATE_TABLE_COORDINATES =
                "CREATE TABLE IF NOT EXISTS " + CoordinatesEntry.TABLE_NAME_COORDINATES + " ( " +
                        CoordinatesEntry._ID + " INTEGER UNIQUE PRIMARY KEY," +
//                        CoordinatesEntry.COLUMN_NODE_ID + " DOUBLE UNIQUE, " +
                        CoordinatesEntry.COLUMN_NODE_LAT + " DOUBLE(12,8), " +
                        CoordinatesEntry.COLUMN_NODE_LNG + " DOUBLE(12,8), " +
                        CoordinatesEntry.COLUMN_WAY_NAME + " VARCHAR(255), " +
                        CoordinatesEntry.COLUMN_NEAR_PLACES + " VARCHAR(255) )";

        static final String SQL_CREATE_TABLE_PLACES =
                "CREATE TABLE IF NOT EXISTS " + CoordinatesEntry.TABLE_NAME_PLACES + " ( " +
                        CoordinatesEntry._ID+" INTEGER PRIMARY KEY," +
                        CoordinatesEntry.COLUMN_PLACE_ID + " DOUBLE UNIQUE, " +
                        CoordinatesEntry.COLUMN_PLACE_NAME + " VARCHAR(255) )";

        static final String SQL_CREATE_TABLE_BOUNDS =
                "CREATE TABLE IF NOT EXISTS " + CoordinatesEntry.TABLE_NAME_BOUNDS+ " ( " +
                        CoordinatesEntry._ID+" INTEGER UNIQUE PRIMARY KEY," +
                        CoordinatesEntry.COLUMN_MAX_LAT + " DOUBLE(12,8), " +
                        CoordinatesEntry.COLUMN_MIN_LAT + " DOUBLE(12,8), " +
                        CoordinatesEntry.COLUMN_MAX_LNG + " DOUBLE(12,8), " +
                        CoordinatesEntry.COLUMN_MIN_LNG + " DOUBLE(12,8) )";

        static final String SQL_DELETE_TABLE_COORDINATES =
                "DROP TABLE IF EXISTS " + CoordinatesEntry.TABLE_NAME_COORDINATES;

        static final String SQL_DELETE_TABLE_BOUNDS =
                "DROP TABLE IF EXISTS " + CoordinatesEntry.TABLE_NAME_BOUNDS;
    }

}
