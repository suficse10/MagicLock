package com.logicbangla.magiclockbt.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by SuFi on 16-Jan-18.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "password_db";
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TableAttributes obj = new TableAttributes();
        String query = obj.tableCreation();

        try{
            db.execSQL(query);
            Log.i("Table Create", "Done!");
        }
        catch(SQLException e){
            Log.e("SQL Error", e.toString());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public String getPassword(int id) {
        String pass_str = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + TableAttributes.USER_PASSWORD + " FROM " + TableAttributes.TABLE_NAME + " WHERE " + TableAttributes.USER_ID + "=" + id;
        Cursor cursor = db.rawQuery(query,null);
        cursor.moveToFirst();
        try {
            if (cursor.getCount() > 0) {
                pass_str = cursor.getString(0);
            }
            //pass_str = db.rawQuery(query, null).toString();
            Log.i("Data Fetch", "Successful!");
        }
        catch (SQLException e){
            Log.e("Data Fetch Error", e.toString());
        }
        finally {
            db.close();
        }
        return pass_str;
    }

    public void insertPassword() {
        ContentValues values = new ContentValues();
        values.put(TableAttributes.USER_PASSWORD, "1234");
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.insert(TableAttributes.TABLE_NAME, null, values);
            Log.i("Data Insertion", "Successful!");
        }
        catch (SQLException e){
            Log.e("Insertion Error", e.toString());
        }
        finally {
            db.close();
        }
    }

    public void updatePassword(String passWord, int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TableAttributes.USER_PASSWORD, passWord);
        try {
            db.update(TableAttributes.TABLE_NAME, values,TableAttributes.USER_ID + "=" + id, null);
            Log.i("Update", "Successful!");
        }
        catch (SQLException e) {
            Log.e("Update Error", e.toString());
        }
        finally {
            db.close();
        }
    }

}
