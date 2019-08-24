package com.camomile.openlibre.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.camomile.openlibre.R;
import com.camomile.openlibre.model.RawTagData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import io.realm.Realm;
import io.realm.Sort;

import static android.content.Context.MODE_PRIVATE;
import static com.camomile.openlibre.OpenLibre.realmConfigRawData;


class CloudStoreUploadAsyncTask extends CloudStoreAsyncTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreUploadAsyncTask.class.getSimpleName();

    CloudStoreUploadAsyncTask(Context context, CloudStoreSynchronization cloudstoreSynchronization) {
        super(context, cloudstoreSynchronization);
    }

    @Override
    public boolean syncData() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null)
            {
                Log.d(LOG_ID, "User is not authenticated");
                return false;
            }

            //TODO research a way to reduce number of queries to Firebase CloudStore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Realm realmProcessedData = Realm.getInstance(realmConfigRawData);

            SharedPreferences preferences = context.getSharedPreferences("cloudstore", MODE_PRIVATE);
            String cloudstoreUploadTimestampKey = preferences.getString("upload_cloudstore_key", "upload_timestamp");
            long cloudstoreUploadTimestamp = preferences.getLong(cloudstoreUploadTimestampKey, 0);

            cloudstoreSynchronization.updateProgress(0, new Date(cloudstoreUploadTimestamp));

            // find data that has not be uploaded yet
            List<RawTagData> newRawData = realmProcessedData.where(RawTagData.class)
                    .greaterThan(RawTagData.DATE, cloudstoreUploadTimestamp)
                    .sort(RawTagData.DATE, Sort.ASCENDING)
                    .findAll();

            int countAllNewRawData = newRawData.size();

            // iterate over glucose data entries and upload them
            for (int i = 0; i < countAllNewRawData; i++) {
                RawTagData rawDataItem = newRawData.get(i);
                String tagid = rawDataItem.getTagId();
                byte[] data = rawDataItem.getData();
                String base64data = new String(Base64.encode(data, Base64.DEFAULT));
                long date = rawDataItem.getDate();

                HashMap<String, Object> dbDataItem = new HashMap<>();
                //dbDataItem.put("timestamp", date);
                dbDataItem.put("i", tagid);
                dbDataItem.put("d", base64data);
                dbDataItem.put("t", date);

                db.collection(currentUser.getUid())
                        .document()
                        .set(dbDataItem);

                cloudstoreUploadTimestamp = date;

                float progress = (i+1) / (float) countAllNewRawData;
                Log.d(LOG_ID, "Uploaded until: " + new Date(cloudstoreUploadTimestamp) + ", progress: " + progress);
                cloudstoreSynchronization.updateProgress(progress, new Date(cloudstoreUploadTimestamp));
            }

            SharedPreferences.Editor preferencesEditor = preferences.edit();
            preferencesEditor.putLong(cloudstoreUploadTimestampKey, cloudstoreUploadTimestamp);
            preferencesEditor.apply();

        } catch (Exception e) {
            Log.e(LOG_ID, "Error: " + e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }
}