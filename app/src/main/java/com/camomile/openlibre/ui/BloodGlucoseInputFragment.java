package com.camomile.openlibre.ui;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.camomile.openlibre.model.BloodGlucoseData;

import java.util.Date;

import com.camomile.openlibre.R;
import io.realm.Realm;

import static com.camomile.openlibre.OpenLibre.realmConfigUserData;

public class BloodGlucoseInputFragment extends DialogFragment {
    public BloodGlucoseInputFragment() {
        // Required empty public constructor
    }

    public static BloodGlucoseInputFragment newInstance() {
        BloodGlucoseInputFragment fragment = new BloodGlucoseInputFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void saveBloodGlucoseLevel(long date, float bloodGlucoseLevel) {
        Realm realmUserData = Realm.getInstance(realmConfigUserData);
        realmUserData.beginTransaction();
        realmUserData.copyToRealmOrUpdate(new BloodGlucoseData(date, bloodGlucoseLevel));
        realmUserData.commitTransaction();
        realmUserData.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(getShowsDialog())
        {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            return inflater.inflate(R.layout.fragment_blood_glucose_input, container, false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_blood_glucose_input, null);
        final EditText editTextGlucose = (EditText) view.findViewById(R.id.edit_text_glucose);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setIcon(R.drawable.ic_sensor);
        alertDialogBuilder.setTitle(getResources().getString(R.string.title_blood_glucose));
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                float bloodGlucoseLevel = Float.valueOf(editTextGlucose.getText().toString());
                // TODO: add user input for measurement time (maybe just as minutes since measurement)
                long date = new Date().getTime();
                saveBloodGlucoseLevel(date, bloodGlucoseLevel);
                dialog.dismiss();
            }
        });
        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        return alertDialogBuilder.create();
    }
}
