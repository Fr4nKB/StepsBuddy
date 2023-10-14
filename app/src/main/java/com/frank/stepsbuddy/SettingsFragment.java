package com.frank.stepsbuddy;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import static com.frank.stepsbuddy.MyUtil.*;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private TextView sex, height, weight, mode, goal, goald, age, tdee;
    private Button modifybtn;
    private SharedPreferences settings;
    public BottomNavigationView navbar;
    public MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle state) {
        View v = inf.inflate(R.layout.settings_fragment, parent, false);

        age = v.findViewById(R.id.age_display);
        sex = v.findViewById(R.id.sex_display);
        height = v.findViewById(R.id.height_display);
        weight = v.findViewById(R.id.weight_display);
        mode = v.findViewById(R.id.mode_display);
        goal = v.findViewById(R.id.goal_label);
        goald = v.findViewById(R.id.goal_display);
        tdee = v.findViewById(R.id.tdee_display);

        modifybtn = v.findViewById(R.id.modifybtn);
        modifybtn.setOnClickListener(modifyPref);

        activity = (MainActivity) getActivity();
        navbar = activity.findViewById(R.id.bottomNavBar);
        settings = activity.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);

        return v;
    }

    public void onStart() {
        super.onStart();
        loadSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        navbar.setVisibility(View.VISIBLE);
    }

    private final View.OnClickListener modifyPref =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentManager fm = activity.getSupportFragmentManager();
                    navbar.setVisibility(View.GONE);
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.slide_l2r_in, R.anim.slide_r2l_out, R.anim.slide_r2l_in, R.anim.slide_l2r_out)
                            .replace(R.id.fragmentHolder, new Setup())
                            .addToBackStack("")
                            .commit();
                }
            };

    private void loadSettings() {
        try {

            age.setText(settings.getString(AGE, ""));

            int id = settings.getInt(SEX, -1);
            if(getActivity().findViewById(id) == activity.findViewById(R.id.radio_male)) sex.setText(R.string.male);
            else sex.setText(R.string.female);

            height.setText(settings.getString(HEIGHT, null));
            weight.setText(settings.getString(WEIGHT, null));

            id = settings.getInt(MODE, -1);
            if(id == R.id.radio_noGeo) mode.setText(R.string.noGeo);
            else mode.setText(R.string.geo);

            id = settings.getInt(GOAL, -1);
            if(activity.findViewById(id) == activity.findViewById(R.id.radio_steps)) goal.setText(R.string.steps);
            else goal.setText(R.string.cal);

            goald.setText(settings.getString(GOALD, ""));
            showTdee();

        }
        catch (Exception ignored) {
        }

    }

    private void showTdee () {
        String lang = Locale.getDefault().getLanguage();
        double weight = Double.parseDouble(settings.getString(WEIGHT, ""));
        if (lang.equals("en") || lang.equals("us")) weight = weight / KG2POUND;
        int height = Integer.parseInt(settings.getString(HEIGHT, ""));
        double age = Double.parseDouble(settings.getString(AGE, ""));

        int id = settings.getInt(SEX, -1);

        if(id == R.id.radio_male) tdee.setText(calcTDEE(CONSTMALE, weight, height, age));
        else tdee.setText(calcTDEE(CONSTFEMALE, weight, height, age));
    }

    private String calcTDEE (double [] array, double weight, double height, double age) {
        int tmp = (int)(array[0] + array[1] * weight + array[2]*height - array[3]*age);
        return Integer.toString(tmp);
    }

}
