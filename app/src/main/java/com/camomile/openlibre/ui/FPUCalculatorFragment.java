package com.camomile.openlibre.ui;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;

import com.camomile.openlibre.R;

public class FPUCalculatorFragment extends DialogFragment {
    private EditText editTextCarbohydrates;
    private EditText editTextCalories;
    private EditText editTextGramCarbohydratesPerUnitInsulin;
    private TextView tvFPU;
    private TextView tvFPUInsulinUnits;
    private TextView tvFPUDelay;
    private TextView tvCarbohydratesInsulinUnits;

    public FPUCalculatorFragment() {
        // Required empty public constructor
    }

    public static FPUCalculatorFragment newInstance() {
        FPUCalculatorFragment fragment = new FPUCalculatorFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void updateFPUCalculation() {
        float carbohydrates, calories, carbsPerUnitInsulin;
        try {
            carbohydrates = Float.parseFloat(editTextCarbohydrates.getText().toString());
            calories = Float.parseFloat(editTextCalories.getText().toString());
            carbsPerUnitInsulin = Float.parseFloat(editTextGramCarbohydratesPerUnitInsulin.getText().toString());
        } catch (NumberFormatException e) {
            return;
        }
        float fpu = Math.max(0.0f, (calories - carbohydrates * 4.0f)) / 100.0f;
        float fpuInsulinUnits = fpu * 12.0f / carbsPerUnitInsulin;
        int fpuDelay = 0;
        if (fpu >= 1.0f) {
            fpuDelay = Math.max(7, (int) Math.floor(fpu + 2));
        }

        tvFPU.setText(String.format(Locale.getDefault(), "%.1f", fpu));
        tvFPUInsulinUnits.setText(String.format(Locale.getDefault(), "%.2f", fpuInsulinUnits));
        tvFPUDelay.setText(String.format(Locale.getDefault(), "%d", fpuDelay));
        tvCarbohydratesInsulinUnits.setText(String.format(Locale.getDefault(), "%.2f", carbohydrates / carbsPerUnitInsulin));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(getShowsDialog())
        {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            View view = inflater.inflate(R.layout.fragment_blood_glucose_input, container, false);
            return view;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_fpu_calculator, null);
        editTextCarbohydrates = (EditText) view.findViewById(R.id.edit_text_carbohydrates);
        editTextCalories = (EditText) view.findViewById(R.id.edit_text_calories);
        editTextGramCarbohydratesPerUnitInsulin = (EditText) view.findViewById(R.id.edit_text_gram_carbohydrates_per_unit_insulin);
        tvCarbohydratesInsulinUnits = (TextView) view.findViewById(R.id.tv_result_carbohydrate_insulin_units);
        tvFPU = (TextView) view.findViewById(R.id.tv_result_fpu);
        tvFPUInsulinUnits = (TextView) view.findViewById(R.id.tv_result_fpu_insulin_units);
        tvFPUDelay = (TextView) view.findViewById(R.id.tv_result_fpu_insulin_delay);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setTitle("FPU Calculator");
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateFPUCalculation();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        editTextCarbohydrates.addTextChangedListener(textWatcher);
        editTextCalories.addTextChangedListener(textWatcher);
        editTextGramCarbohydratesPerUnitInsulin.addTextChangedListener(textWatcher);

        return alertDialogBuilder.create();
    }
}
