package com.frank.stepsbuddy;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import static com.frank.stepsbuddy.MyUtil.*;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StatsFragment extends Fragment {

    private int numSavedPaths;
    private SharedPreferences settings;
    private Activity activity;
    private TableLayout pathContainer;
    private GraphView graphSteps, graphCal;
    private static Database db;
    private DataPoint[] dp = new DataPoint[7];
    private LineGraphSeries<DataPoint> series;
    private TableRow btnRow;
    private TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 200);
    private TableRow.LayoutParams tvlp1 = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
    private TableRow.LayoutParams tvlp2 = new TableRow.LayoutParams(200, TableRow.LayoutParams.WRAP_CONTENT);
    private TableRow.LayoutParams tvlp2h = new TableRow.LayoutParams(250, TableRow.LayoutParams.WRAP_CONTENT);
    private TableRow.LayoutParams tvlp3 = new TableRow.LayoutParams(160, TableRow.LayoutParams.WRAP_CONTENT);


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        db = new Database(activity, 1);
        settings = activity.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        numSavedPaths = settings.getInt(NPATHSAVED, 0);

    }

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle state) {
        View v = inf.inflate(R.layout.stats_fragment, parent, false);

        graphSteps = v.findViewById(R.id.stepsGraph);
        graphCal = v.findViewById(R.id.calGraph);
        loadGraphSettings(graphSteps);
        loadGraphSettings(graphCal);

        pathContainer = v.findViewById(R.id.pathContainer);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        loadGraphData(graphSteps,0);
        loadGraphData(graphCal,1);

        if(numSavedPaths > 0) {
            pathContainer.setVisibility(View.VISIBLE);
            loadPathsaved();
        }
        else pathContainer.setVisibility(View.GONE);

    }

    private void loadGraphSettings(GraphView g) {
        g.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Format formatter = new SimpleDateFormat("dd/MM");
                    return formatter.format(value);
                }
                return super.formatLabel(value, isValueX);
            }
        });
        g.getGridLabelRenderer().setNumHorizontalLabels(7);
        g.getGridLabelRenderer().setHumanRounding(false);
    }

    private void loadGraphData(GraphView g, int choice) {
        int data = 0;
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        for(int i = 0; i < 7; i++) {
            data = db.read(convertDate(cal.getTime()), choice);
            dp[i] = new DataPoint(cal.getTime(), data);
            cal.add(Calendar.DATE, 1);
        }

        series = new LineGraphSeries<>(dp);
        g.addSeries(series);
    }

    private void addTextView(ViewGroup vg, String content, LinearLayout.LayoutParams lp, int alignment, int size) {
        TextView t = new TextView(vg.getContext());
        t.setText(content);

        t.setTextSize((float) size);
        t.setGravity(alignment);
        t.setLayoutParams(lp);
        vg.addView(t);
    }

    private void addRow(ViewGroup vg, String[] texts, LinearLayout.LayoutParams lp) {
        TableRow row = new TableRow(activity);
        row.setLayoutParams(lp);
        row.setMinimumHeight(200);
        row.setGravity(Gravity.CENTER_VERTICAL);

        for(int i = 0; i < 4; i++) {
            if(i == 0) addTextView(row, texts[i], tvlp1, Gravity.START, 12);
            else addTextView(row, texts[i], tvlp2h, Gravity.CENTER, 12);
        }

        TextView t = new TextView(vg.getContext());
        t.setText(texts[4]);
        t.setVisibility(View.GONE);
        row.addView(t);

        row.setOnClickListener(handlerShow);
        vg.addView(row);
    }

    private void loadPathsaved() {

        TableRow headers = new TableRow(activity);
        headers.setLayoutParams(lp);
        headers.setMinimumHeight(50);
        String[] texts = new String[] {getString(R.string.namecol),getString(R.string.datacol),getString(R.string.distcol),getString(R.string.timecol)};

        for(int i = 0; i < 4; i++) {
            if(i == 0) addTextView(headers, texts[i], tvlp1, Gravity.START, 15);
            else if(i == 2) addTextView(headers, texts[i], tvlp2, Gravity.CENTER, 15);
            else addTextView(headers, texts[i], tvlp3, Gravity.CENTER, 15);
        }

        pathContainer.addView(headers);

        for(int i = 0; i < numSavedPaths; i++) {
            PathSlot p = db.readPath(i);

            if(p != null) {
                addRow(pathContainer, new String[]{p.name, p.data, p.dist, p.time, p.pn}, lp);
            }

        }

    }

    private Button createBtn(int text, View.OnClickListener handler) {
        Button btn = new Button(activity);
        btn.setText(text);
        btn.setOnClickListener(handler);

        return btn;
    }

    private View.OnClickListener handlerShow = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int pos = pathContainer.indexOfChild(view);

            btnRow = new TableRow(activity);
            btnRow.setLayoutParams(lp);
            btnRow.setMinimumHeight(100);
            btnRow.setGravity(Gravity.CENTER);

            Button b1, b2, b3;
            b1 = createBtn(R.string.del, btnHandler);
            b2 = createBtn(R.string.load, btnHandler);
            b3 = createBtn(R.string.share, btnHandler);

            btnRow.addView(b1);
            btnRow.addView(b2);
            btnRow.addView(b3);

            view.setOnClickListener(handlerHide);
            pathContainer.addView(btnRow, pos+1);

        }
    };

    private View.OnClickListener handlerHide = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int pos = pathContainer.indexOfChild(view);

            view.setOnClickListener(handlerShow);
            pathContainer.removeView(pathContainer.getChildAt(pos+1));

        }
    };

    private static class btnTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            if(strings[0].equals("0")) {
                db.delPath(strings[1]);
            }

            else db.loadPath(strings[1]);

            return null;
        }

    }

    private View.OnClickListener btnHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TableRow row = (TableRow) view.getParent();
            int pos = pathContainer.indexOfChild(row) - 1;
            TextView pntv = (TextView)((TableRow) pathContainer.getChildAt(pos)).getChildAt(4);
            String pn = pntv.getText().toString();
            String text = ((Button) view).getText().toString();

            if(text.equals(getString(R.string.del))) {
                new btnTask().execute("0", pn);
                pathContainer.removeViewAt(pos);
                pathContainer.removeViewAt(pos);
            }

            else if(text.equals(getString(R.string.load))) {
                clearList();
                new btnTask().execute("1", pn);
                pathContainer.removeViewAt(pos+1);
                loadMode = true;
            }

            else {

            }

        }
    };

}
