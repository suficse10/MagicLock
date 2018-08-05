package com.logicbangla.magiclockbt.database;

/**
 * Created by SuFi on 16-Jan-18.
 */

public class TableAttributes {
    public static final String TABLE_NAME = "user_table";
    public static final String USER_ID = "id";
    public static final String USER_NAME = "name";
    public static final String USER_PASSWORD = "password";

    public String tableCreation() {

        String query = "CREATE TABLE " + TABLE_NAME + " (" + USER_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT , "
                + USER_PASSWORD + " TEXT );";
        return query;
    }
}
