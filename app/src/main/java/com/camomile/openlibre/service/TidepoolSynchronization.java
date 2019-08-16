package com.camomile.openlibre.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

public class TidepoolSynchronization {
    private static final String LOG_ID = "OpenLibre::" + TidepoolSynchronization.class.getSimpleName();

    private static TidepoolSynchronization instance;

    private TidepoolUploadTask tidepoolUploadTask;
    private float progress;
    private Date progressDate;
    private boolean synchronizationRunning;

    public interface ProgressCallBack {
        void updateProgress(float progress, Date currentDate);
        void finished();
    }
    private ProgressCallBack progressCallBack;

    private TidepoolSynchronization() {
        progress = 0;
        progressDate = new Date();
        synchronizationRunning = false;
    }

    public static synchronized TidepoolSynchronization getInstance() {
        if(instance == null){
            instance = new TidepoolSynchronization();
        }
        return instance;
    }

    void updateProgress(float progress, Date progressDate) {
        this.progress = progress;
        this.progressDate = progressDate;
        if (progressCallBack != null) {
            progressCallBack.updateProgress(progress, progressDate);
        }
    }

    void finished() {
        synchronizationRunning = false;
        tidepoolUploadTask = null;
        if (progressCallBack != null) {
            progressCallBack.finished();
        }
    }

    public void registerProgressUpdateCallback(TidepoolSynchronization.ProgressCallBack progressCallBack) {
        this.progressCallBack = progressCallBack;
        progressCallBack.updateProgress(progress, progressDate);
    }

    public void unregisterProgressUpdateCallback() {
        progressCallBack = null;
    }

    public boolean isSynchronizationRunning() {
        return synchronizationRunning;
    }

    public void cancelSynchronization() {
        if (tidepoolUploadTask != null) {
            tidepoolUploadTask.cancel(false);
        }
    }

    public void startManualSynchronization(Context context) {
        if (tidepoolUploadTask == null) {
            Log.d(LOG_ID, "starting new sync task");
            tidepoolUploadTask = new TidepoolUploadTask(context, this);
            tidepoolUploadTask.execute();
            synchronizationRunning = true;
        }
    }

    public void startTriggeredSynchronization(Context context) {
        Log.d(LOG_ID, "startTriggeredSynchronization");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autoSync = settings.getBoolean("pref_tidepool_auto_sync", false);
        if (!autoSync) {
            Log.d(LOG_ID, "not syncing: auto sync is disabled");
            return;
        }

        Log.d(LOG_ID, "checking connection");
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Log.d(LOG_ID, "not syncing: not connected");
            return;
        }

        Log.d(LOG_ID, "checking connection type");
        int connectionType = activeNetwork.getType();
        boolean isMobile = connectionType == ConnectivityManager.TYPE_MOBILE || connectionType == ConnectivityManager.TYPE_MOBILE_DUN;

        if (isMobile) {
            boolean autoSyncMobile = settings.getBoolean("pref_tidepool_auto_sync_mobile", false);
            if (!autoSyncMobile) {
                Log.d(LOG_ID, "not syncing: mobile connection and auto sync mobile is disabled");
                return;
            }
        }

        startManualSynchronization(context);
    }
}
