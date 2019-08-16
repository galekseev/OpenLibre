package com.camomile.openlibre.ui;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.camomile.openlibre.service.TidepoolSynchronization;

import java.text.DateFormat;
import java.util.Date;

import com.camomile.openlibre.R;
import io.tidepool.api.APIClient;
import io.tidepool.api.data.User;

import static android.content.Context.MODE_PRIVATE;

public class TidepoolStatusFragment extends DialogFragment implements TidepoolSynchronization.ProgressCallBack {
    private Button buttonSyncOrCancel;
    private ProgressBar progressBarSync;
    private TextView textViewSyncStatus;
    private DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public TidepoolStatusFragment() {
        // Required empty public constructor
    }

    public static TidepoolStatusFragment newInstance() {
        TidepoolStatusFragment fragment = new TidepoolStatusFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(getShowsDialog())
        {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            return inflater.inflate(R.layout.fragment_tidepool_status, container, false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_tidepool_status, null);
        final ProgressBar progressBarLogin = (ProgressBar) view.findViewById(R.id.pb_login);
        progressBarSync = (ProgressBar) view.findViewById(R.id.pb_sync);
        textViewSyncStatus = (TextView) view.findViewById(R.id.tv_sync_status);
        buttonSyncOrCancel = (Button) view.findViewById(R.id.button_sync_or_cancel);

        final TextView textViewAccount = (TextView) view.findViewById(R.id.tv_account);
        final TidepoolSynchronization tidepoolSynchronization = TidepoolSynchronization.getInstance();
        tidepoolSynchronization.registerProgressUpdateCallback(this);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        String tidepoolUsername = settings.getString("pref_tidepool_username", "");
        String tidepoolPassword = settings.getString("pref_tidepool_password", "");
        String tidepoolServer = settings.getString("pref_tidepool_server", APIClient.PRODUCTION);

        SharedPreferences preferences = getContext().getSharedPreferences("tidepool", MODE_PRIVATE);
        String tidepoolUploadTimestampKey = preferences.getString("upload_timestamp_key", "upload_timestamp");
        long tidepoolUploadTimestamp = preferences.getLong(tidepoolUploadTimestampKey, 0);

        if (tidepoolUploadTimestamp > 0) {
            textViewSyncStatus.setText(
                    String.format(getString(R.string.synchronized_until),
                            dateTimeFormat.format(new Date(tidepoolUploadTimestamp))));
        } else {
            textViewSyncStatus.setText(getString(R.string.not_synchronized));
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(view);
        // TODO: alertDialogBuilder.setIcon(R.drawable.ic_tidepool);
        alertDialogBuilder.setTitle("Tidepool");

        buttonSyncOrCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (tidepoolSynchronization.isSynchronizationRunning()) {

                    tidepoolSynchronization.cancelSynchronization();
                    buttonSyncOrCancel.setEnabled(false);
                } else {

                    progressBarSync.setProgress(0);
                    progressBarSync.setVisibility(View.VISIBLE);
                    buttonSyncOrCancel.setText(getString(R.string.pause));
                    tidepoolSynchronization.startManualSynchronization(getContext());
                }
            }
        });

        APIClient tidepoolAPIClient = new APIClient(getContext(), tidepoolServer);
        progressBarLogin.setVisibility(View.VISIBLE);
        tidepoolAPIClient.signIn(tidepoolUsername, tidepoolPassword, new APIClient.SignInListener() {
            @Override
            public void signInComplete(User user, Exception exception) {
                progressBarLogin.setVisibility(View.INVISIBLE);
                if (exception != null) {
                    textViewAccount.setText(getString(R.string.login_failed));
                    buttonSyncOrCancel.setEnabled(false);
                } else {
                    textViewAccount.setText(user.getUsername());
                    buttonSyncOrCancel.setEnabled(true);
                }
            }
        });

        return alertDialogBuilder.create();
    }

    @Override
    public void onDestroyView() {
        TidepoolSynchronization.getInstance().unregisterProgressUpdateCallback();
        super.onDestroyView();
    }

    @Override
    public void updateProgress(final float progress, final Date currentDate) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (TidepoolSynchronization.getInstance().isSynchronizationRunning()) {
                    buttonSyncOrCancel.setText(getString(R.string.pause));
                    progressBarSync.setVisibility(View.VISIBLE);
                } else {
                    buttonSyncOrCancel.setText(getString(R.string.synchronize));
                    progressBarSync.setVisibility(View.INVISIBLE);
                }
                progressBarSync.setProgress((int) (100 * progress));
                textViewSyncStatus.setText(String.format(getString(R.string.synchronized_until), dateTimeFormat.format(currentDate)));
            }
        });
    }

    @Override
    public void finished() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                buttonSyncOrCancel.setText(getString(R.string.synchronize));
                buttonSyncOrCancel.setEnabled(true);
                progressBarSync.setVisibility(View.INVISIBLE);
            }
        });
    }
}
