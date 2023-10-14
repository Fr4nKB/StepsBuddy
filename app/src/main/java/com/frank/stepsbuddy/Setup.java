package com.frank.stepsbuddy;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import static com.frank.stepsbuddy.MyUtil.*;


public class Setup extends Fragment {

    private Button savebtn;
    private RadioGroup sex, mode, goal;
    private EditText height, weight, goald, age;
    public MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup parent, Bundle state) {
        View v = inf.inflate(R.layout.setup_fragment, parent, false);

        savebtn = v.findViewById(R.id.savebtn);
        savebtn.setOnClickListener(savePref);

        age = v.findViewById(R.id.ageInput);
        age.addTextChangedListener(textChecker);
        
        sex = v.findViewById(R.id.sex);
        sex.setOnCheckedChangeListener(radioChecker);

        mode = v.findViewById(R.id.mode);
        mode.setOnCheckedChangeListener(radioChecker);

        height = v.findViewById(R.id.heightInput);
        height.addTextChangedListener(textChecker);

        weight = v.findViewById(R.id.weightInput);
        weight.addTextChangedListener(textChecker);

        goal = v.findViewById(R.id.goal);
        goal.setOnCheckedChangeListener(radioChecker);
        goald = v.findViewById(R.id.goalInput);
        goald.addTextChangedListener(textChecker);

        activity = (MainActivity) getActivity();
        activity.findViewById(R.id.bottomNavBar).setVisibility(View.GONE);

        return v;
    }

    public void onStart() {
        super.onStart();
        loadSettings();
        savebtn.setEnabled(false);
    }

    private void saveSettings() {
        SharedPreferences settings = getActivity().getSharedPreferences(SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(AGE, age.getText().toString());
        editor.putInt(SEX, sex.getCheckedRadioButtonId());
        editor.putString(HEIGHT, height.getText().toString());
        editor.putString(WEIGHT, weight.getText().toString());
        editor.putInt(MODE, mode.getCheckedRadioButtonId());
        editor.putInt(GOAL, goal.getCheckedRadioButtonId());
        editor.putString(GOALD, goald.getText().toString());

        editor.apply();
    }

    private void setRadio(SharedPreferences settings, String key, RadioGroup rg) {
        int tmp = settings.getInt(key, -1);
        if(tmp != -1) {
            RadioButton s = rg.findViewById(tmp);
            s.setChecked(true);
        }
    }

    private void loadSettings() {
        try {
            SharedPreferences settings = getActivity().getSharedPreferences(SETTINGS, MODE_PRIVATE);
            age.setText(settings.getString(AGE, ""));
            height.setText(settings.getString(HEIGHT, ""));
            weight.setText(settings.getString(WEIGHT, ""));
            goald.setText(settings.getString(GOALD, ""));

            setRadio(settings, SEX, sex);
            setRadio(settings, MODE, mode);
            setRadio(settings, GOAL, goal);

        }
        catch (Exception ignored) {
        }

    }

    private final View.OnClickListener savePref =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveSettings();
                    savebtn.setEnabled(false);

                    SharedPreferences settings = getActivity().getSharedPreferences(SETTINGS, MODE_PRIVATE);

                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_r2l_in, R.anim.slide_l2r_out);

                    if(settings.getBoolean(FIRSTBOOT, true)) {
                        settings.edit().putBoolean(FIRSTBOOT, false).putString(CURRENTDATE, MyUtil.getCurrentDate()).apply();
                        ft.replace(R.id.fragmentHolder, new HomeFragment());
                        activity.navbar.setSelectedItemId(R.id.homeTab);
                    }

                    else fm.popBackStack();

                    ft.commit();

                    activity.navbar.setVisibility(View.VISIBLE);

                }
            };

    private final RadioGroup.OnCheckedChangeListener radioChecker =
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    globalcheck();
                }
            };

    private final TextWatcher textChecker = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
            if(age.getText().toString().length() == 0 || height.getText().toString().length() == 0
                    || weight.getText().toString().length() == 0) savebtn.setEnabled(false);
            else globalcheck();
        }
    };

    private void globalcheck() {
        savebtn.setEnabled(height.getText().toString().length() != 0
                && weight.getText().toString().length() != 0
                && goald.getText().toString().length() != 0);
    }

}
