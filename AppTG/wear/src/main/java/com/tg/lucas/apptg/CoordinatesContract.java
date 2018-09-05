package com.tg.lucas.apptg;

import android.provider.BaseColumns;

public final class CoordinatesContract {

    private CoordinatesContract(){

    }

    public static class CoordinatesEntry implements BaseColumns{
        public static final String TABLE_NAME = "coordinates";
        public static final String COLUMN_NODE_ID = "node_id";
        public static final String COLUMN_NODE_LAT = "node_lat";
        public static final String COLUMN_NODE_LNG = "node_lng";
        public static final String COLUMN_WAY_NAME = "way_name";

        public static final String SQL_CREATE_DATABASE =
                "CREATE TABLE IF NOT EXISTS " + CoordinatesEntry.TABLE_NAME + " ( " +
                CoordinatesEntry._ID+" INTEGER UNIQUE PRIMARY KEY," +
                CoordinatesEntry.COLUMN_NODE_ID + " UNSIGNED INT(11), " +
                CoordinatesEntry.COLUMN_NODE_LAT + " DOUBLE(12,8), " +
                CoordinatesEntry.COLUMN_NODE_LNG + " DOUBLE(12,8), " +
                CoordinatesEntry.COLUMN_WAY_NAME + " VARCHAR(255) )";

        public static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + CoordinatesEntry.TABLE_NAME;
    }

}
