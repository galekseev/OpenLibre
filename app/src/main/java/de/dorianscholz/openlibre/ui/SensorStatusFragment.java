package de.dorianscholz.openlibre.ui;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.dorianscholz.openlibre.R;
import de.dorianscholz.openlibre.model.SensorData;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static de.dorianscholz.openlibre.OpenLibre.realmConfigProcessedData;
import static de.dorianscholz.openlibre.model.AlgorithmUtil.getDurationBreakdown;
import static de.dorianscholz.openlibre.model.SensorData.START_DATE;

public class SensorStatusFragment extends DialogFragment {
    public SensorStatusFragment() {
        // Required empty public constructor
    }

    public static SensorStatusFragment newInstance() {
        SensorStatusFragment fragment = new SensorStatusFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void setupUi(View view) {
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);
        RealmResults<SensorData> sensorDataResults = realmProcessedData.where(SensorData.class).
                sort(START_DATE, Sort.DESCENDING).findAll();

        TextView sensorId = (TextView) view.findViewById(R.id.tv_sensor_id_value);
        TextView sensorStartDate = (TextView) view.findViewById(R.id.tv_sensor_start_date_value);
        TextView sensorEndsIn = (TextView) view.findViewById(R.id.tv_sensor_ends_in_value);

        if (sensorDataResults.size() == 0) {
            sensorId.setText(getResources().getString(R.string.no_sensor_registered));
            sensorStartDate.setText("");
            sensorEndsIn.setText("");
        } else {
            SensorData sensorData = sensorDataResults.first();
            sensorId.setText(sensorData.getTagId());
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            sensorStartDate.setText(dateFormat.format(new Date(sensorData.getStartDate())));
            long timeLeft = sensorData.getTimeLeft();
            if (timeLeft >= TimeUnit.MINUTES.toMillis(1L)) {
                sensorEndsIn.setText(getDurationBreakdown(getResources(), sensorData.getTimeLeft()));
            } else {
                sensorEndsIn.setText(getResources().getString(R.string.sensor_expired));
            }
        }
        realmProcessedData.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(getShowsDialog())
        {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            View view = inflater.inflate(R.layout.fragment_sensor_status, container, false);
            setupUi(view);
            return view;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_sensor_status, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setIcon(R.drawable.ic_sensor);
        alertDialogBuilder.setTitle(getResources().getString(R.string.title_sensor_status));
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        setupUi(view);

        return alertDialogBuilder.create();
    }
}
