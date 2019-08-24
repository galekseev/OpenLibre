package com.camomile.openlibre.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

public class CloudStoreSynchronization {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreSynchronization.class.getSimpleName();
    private static boolean AUTOSYNC_ENABLED_DEFAULT = true;
    private static boolean AUTOSYNC_MOBILE_DEFAULT = true;

    private static CloudStoreSynchronization instance;

    private CloudStoreAsyncTask cloudstoreUploadTask;
    private float progress;
    private Date progressDate;
    private boolean synchronizationRunning;

    public interface ProgressCallBack {
        void updateProgress(float progress, Date currentDate);
        void finished();
    }
    private ProgressCallBack progressCallBack;

    private CloudStoreSynchronization() {
        progress = 0;
        progressDate = new Date();
        synchronizationRunning = false;
    }

    public static synchronized CloudStoreSynchronization getInstance() {
        if(instance == null){
            instance = new CloudStoreSynchronization();
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
        cloudstoreUploadTask = null;
        if (progressCallBack != null) {
            progressCallBack.finished();
        }
    }

    public void registerProgressUpdateCallback(CloudStoreSynchronization.ProgressCallBack progressCallBack) {
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
        if (cloudstoreUploadTask != null) {
            cloudstoreUploadTask.cancel(false);
        }
    }

    public void startManualUpload(Context context) {
        if (cloudstoreUploadTask == null) {
            Log.d(LOG_ID, "starting new sync task");
            cloudstoreUploadTask = new CloudStoreUploadAsyncTask(context, this);
            cloudstoreUploadTask.execute();
            synchronizationRunning = true;
        }
    }

    public void startManualDownload(Context context) {

    }

    public void startTriggeredUpload(Context context) {
        Log.d(LOG_ID, "startTriggeredUpload");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autoSync = settings.getBoolean("pref_cloudstore_auto_sync", AUTOSYNC_ENABLED_DEFAULT);
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
            boolean autoSyncMobile = settings.getBoolean("pref_cloudstore_auto_sync_mobile", AUTOSYNC_MOBILE_DEFAULT);
            if (!autoSyncMobile) {
                Log.d(LOG_ID, "not syncing: mobile connection and auto sync mobile is disabled");
                return;
            }
        }

        startManualUpload(context);
    }
}