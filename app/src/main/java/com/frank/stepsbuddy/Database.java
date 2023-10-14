package com.frank.stepsbuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;
import static com.frank.stepsbuddy.MyUtil.*;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {

    public Database(@Nullable Context context, int version) {
        super(context, DBNAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createStatement = "CREATE TABLE record (" +
                "data VARCHAR(10) PRIMARY KEY, steps INT, calories INT)";

        db.execSQL(createStatement);

        createStatement = "CREATE TABLE path (" +
                "pathNum INT PRIMARY KEY, name VARCHAR(255), data VARCHAR(10), distanceMT INT, timeMIN INT)";

        db.execSQL(createStatement);

        createStatement = "CREATE TABLE coordinates (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, pathNum INT, code INT, lat Decimal(8,6), long Decimal(9,6))";

        db.execSQL(createStatement);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        ;
    }

    private boolean add(String TABLENAME, ContentValues cv) {
        boolean res = false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            res = db.replace(TABLENAME, null, cv) != -1;
            db.close();
        }
        catch (Exception ignored) {}

        return res;
    }

    public boolean addProgress(int steps, int calories) {
        ContentValues cv = new ContentValues();

        cv.put(DATACOLUMN, MyUtil.getCurrentDate());
        cv.put(STEPSCOLUMN, steps);
        cv.put(CALCOLUMN, calories);

        return add(MAINTABLE, cv);
    }

    public boolean addPath(int pathNum, String name, int distance, int time){
        ContentValues cv = new ContentValues();

        cv.put(PNCOLUMN, pathNum);
        cv.put(NAMECOLUMN, name);
        cv.put(DATACOLUMN, getCurrentDate());
        cv.put(DISTCOLUMN, distance);
        cv.put(TIMECOLUMN, time);

        return add(PATHTABLE, cv);
    }

    public boolean addCoordinates(int pathNum, int code, double lat, double lng){
        ContentValues cv = new ContentValues();

        cv.put(PNCOLUMN, pathNum);
        cv.put(CODECOLUMN, code);
        cv.put(LATCOLUMN, lat);
        cv.put(LONGCOLUMN, lng);

        return add(COORDTABLE, cv);
    }

    public int read(String date, int choice) {
        int tmp = 0;

        String[] column;
        if(choice == 0) column = new String[] {STEPSCOLUMN};
        else column = new String[] {CALCOLUMN};

        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.query(MAINTABLE,
                    column,
                    "data = ?",
                    new String[]{date},
                    null, null, null);

            while (c.moveToNext()) {
                int index = c.getColumnIndexOrThrow(column[0]);
                tmp = Integer.parseInt(c.getString(index));
            }

            c.close();
            db.close();
        }
        catch (Exception e) { return 0;}

        return tmp;

    }

    public PathSlot readPath(int pathNum) {

        PathSlot p = null;

        String[] column = new String[] {PNCOLUMN, NAMECOLUMN, DATACOLUMN, DISTCOLUMN, TIMECOLUMN};

        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.query(PATHTABLE,
                    column,
                    "pathNum = ?",
                    new String[]{Integer.toString(pathNum)},
                    null, null, null);

            String pn =  "", nam = "", data = "", dist = "", time = "";
            int index;

            while(c.moveToNext()) {
                index = c.getColumnIndexOrThrow(column[0]);
                pn = c.getString(index);
                index = c.getColumnIndexOrThrow(column[1]);
                nam = c.getString(index);
                index = c.getColumnIndexOrThrow(column[2]);
                data = c.getString(index);
                index = c.getColumnIndexOrThrow(column[3]);
                dist = c.getString(index);
                index = c.getColumnIndexOrThrow(column[4]);
                time = c.getString(index);

                p = new MyUtil.PathSlot(new String[] {pn, nam, data, dist, time});
            }

            c.close();
            db.close();

            return p;

        }
        catch (Exception e) { return null; }

    }

    public boolean delPath(String pathNum) {
        try {

            SQLiteDatabase db = getWritableDatabase();
            db.delete(PATHTABLE, PNCOLUMN + " = ?", new String[] {pathNum});
            db.delete(COORDTABLE, PNCOLUMN + " = ?", new String[] {pathNum});

            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public void loadPath(String pathNum) {

        try {

            String[] column = new String[] {CODECOLUMN, LATCOLUMN, LONGCOLUMN};

            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.query(COORDTABLE,
                    column,
                    "pathNum = ?",
                    new String[]{pathNum},
                    null, null, IDCOLUMN + " ASC");

            double lat = 0, lon = 0;
            String tmp;
            ArrayList<LatLng> list = new ArrayList<LatLng>();
            int lastCode = 0;
            int index;

            paths = new ArrayList<ArrayList<LatLng>>();

            while(c.moveToNext()) {
                Log.w("test", "sdasd");
                index = c.getColumnIndexOrThrow(column[0]);
                tmp = c.getString(index);

                if(lastCode != Integer.parseInt(tmp)) {
                    paths.add(list);
                    list = new ArrayList<LatLng>();
                    lastCode = Integer.parseInt(tmp);
                }

                index = c.getColumnIndexOrThrow(column[1]);
                lat = Double.parseDouble(c.getString(index));

                index = c.getColumnIndexOrThrow(column[2]);
                lon = Double.parseDouble(c.getString(index));

                list.add(new LatLng(lat, lon));

            }

            paths.add(list);

            c.close();
            db.close();

        }
        catch (Exception ignored) {}

    }

}
