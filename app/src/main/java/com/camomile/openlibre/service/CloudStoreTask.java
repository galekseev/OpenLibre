package com.camomile.openlibre.service;

import android.os.AsyncTask;
import android.util.Log;


public abstract class CloudStoreTask extends AsyncTask<Void, Void, Boolean> {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreTask.class.getSimpleName();

    public static final int NO_ERRORS = -1;
    public static final int UNKNOWN_ERROR = 0;
            ;
    protected ITaskContainer taskContainer;
    protected int errorCode = NO_ERRORS;

    CloudStoreTask(ITaskContainer taskContainer) {
        this.taskContainer = taskContainer;
    }

    public int getErrorCode(){
        return errorCode;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if( taskContainer != null)
            if (success)
                taskContainer.onTaskSuccess(this);
            else{
                if (errorCode == NO_ERRORS) errorCode = UNKNOWN_ERROR;
                taskContainer.onTaskError(this);
            }
    }

    @Override
    protected void onCancelled() {
        if (taskContainer != null)
            taskContainer.onTaskCancelled(this);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Log.v(LOG_ID, String.format("Start %s task", getTaskType()));
        boolean result = doWork();
        Log.v(LOG_ID, String.format("Finished %s task with result: %s", getTaskType(), result));
        return result;
    }

    public abstract boolean doWork();
    public abstract String getTaskType();
    public abstract Object getResult();
}