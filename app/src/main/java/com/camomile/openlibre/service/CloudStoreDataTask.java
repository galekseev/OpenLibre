package com.camomile.openlibre.service;

import android.content.Context;
import android.content.OperationApplicationException;
import android.util.Log;
import android.widget.Toast;

import com.camomile.openlibre.OpenLibre;
import com.camomile.openlibre.R;
import com.camomile.openlibre.model.db.UserProfile;


abstract class CloudStoreDataTask extends CloudStoreTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreDataTask.class.getSimpleName();

    Context context;

    CloudStoreDataTask(Context context, CloudStoreSynchronization cloudstoreSynchronization) {
        super(cloudstoreSynchronization);
        this.context = context;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            Toast.makeText(context,
                    context.getString(R.string.cloudstore_sync_success),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(context,
                    context.getString(R.string.cloudstore_sync_error),
                    Toast.LENGTH_SHORT
            ).show();
        }
        taskContainer.onTaskSuccess(this);
    }

    protected static String getCollectionId()
    {
//        FirebaseAuth auth = FirebaseAuth.getInstance();
//        FirebaseUser currentUser = auth.getCurrentUser();

//        if (currentUser == null)
//        {
//            Log.d(LOG_ID, "User is not authenticated");
//            return null;
//        }

        if (OpenLibre.userProfile == null || OpenLibre.userProfile.getType() != UserProfile.AccountType.LINKED)
            return "currentUser.getUid()";
        else
            return OpenLibre.userProfile.getMasterUid();
    }

    @Override
    public Object getResult(){
        return null;
    }
}