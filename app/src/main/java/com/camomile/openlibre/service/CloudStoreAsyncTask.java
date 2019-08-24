package com.camomile.openlibre.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.camomile.openlibre.R;
import com.camomile.openlibre.model.RawTagData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.Sort;

import static android.content.Context.MODE_PRIVATE;
import static com.camomile.openlibre.OpenLibre.realmConfigRawData;


abstract class CloudStoreAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreAsyncTask.class.getSimpleName();

    Context context;
    CloudStoreSynchronization cloudstoreSynchronization;

    CloudStoreAsyncTask(Context context, CloudStoreSynchronization cloudstoreSynchronization) {
        this.context = context;
        this.cloudstoreSynchronization = cloudstoreSynchronization;
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
        cloudstoreSynchronization.finished();
    }

    @Override
    protected void onCancelled() {
        cloudstoreSynchronization.finished();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return syncData();
    }

    public abstract boolean syncData();
}