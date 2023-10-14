package com.frank.stepsbuddy;

import android.graphics.Color;
import android.util.Log;
import android.widget.TableRow;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyUtil {
    public static final String SEX = "sex", MODE = "mode", GOAL = "goalmode", GOALD = "goald", NPATHSAVED = "numberpathsaved",
            HEIGHT = "height", WEIGHT = "weight", FIRSTBOOT = "firstboot", APP_NAME = "StepsBuddy", CAL = "calories",
            SERVICE_CHANNEL_NAME = "MainService", CHANNEL_ID = "Main Service", AGE = "age", STEPS = "steps",
            STARTBTN = "startbtn", MIDNIGHT_RESET = "midnightR", SETTINGS = "settings", GOALUPDATE = "goalupdate",
            CURRENTDATE = "currentdate", DBNAME = "stepsbuddy", DATACOLUMN = "data", STEPSCOLUMN = "steps",
            CALCOLUMN = "calories", MAINTABLE = "record", PATHTABLE = "path", COORDTABLE = "coordinates", PNCOLUMN = "pathNum",
            DISTCOLUMN = "distanceMT", TIMECOLUMN = "timeMIN", CODECOLUMN = "code", LATCOLUMN = "lat", LONGCOLUMN = "long",
            NAMECOLUMN = "name", IDCOLUMN = "ID";
    public static final double KG2POUND = 2.20462, STEPMALE = 0.415/100000, STEPFEMALE = 0.413/100000;
    public static final double [] CONSTMALE = new double[]{66.47, 13.75, 5, 6.76};
    public static final double [] CONSTFEMALE = new double[]{655.1, 9.56, 1.85, 4.68};
    public static final int SERVICE_NOTIF_ID = 69, PERM_CODE = 1, BL_CODE = 2, MAXNUMSAVEDPATH = 10;
    public static ArrayList<ArrayList<LatLng>> paths = new ArrayList<ArrayList<LatLng>>();
    public static boolean loadMode = false;

    public static String convertDate(Date d) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yy");
        return f.format(d);
    }

    public static String getCurrentDate() {
        return new SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                .format(new Date(System.currentTimeMillis()));
    }

    public static void drawPolyLineOnMap(List<LatLng> list, GoogleMap map, int color) {
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(color);
        polyOptions.width(5);
        polyOptions.addAll(list);

        try {
            map.addPolyline(polyOptions);
        }
        catch (Exception ignored){}
    }

    public static class PathSlot {
        String pn, name, data, dist, time;

        public PathSlot(String[] s) {
            this.pn = s[0];
            this.name = s[1];
            this.data = s[2];
            this.dist = s[3];
            this.time = s[4];
        }

    }

    public static void clearList() {
        for(int i = 0; i < paths.size(); i++) {
            paths.get(i).clear();
        }

        paths.clear();
    }

}
