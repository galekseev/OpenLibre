package com.camomile.openlibre.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.camomile.openlibre.model.RawTagData;

import io.realm.Realm;
import io.realm.Sort;

import static android.content.Context.MODE_PRIVATE;
import static com.camomile.openlibre.OpenLibre.realmConfigRawData;

class CloudStoreUploadDataTask extends CloudStoreDataTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreUploadDataTask.class.getSimpleName();
    public static final String TASK_TYPE = "UPLOAD_NEW_DATA";

    CloudStoreUploadDataTask(Context context, CloudStoreSynchronization cloudstoreSynchronization) {
        super(context, cloudstoreSynchronization);
    }

    @Override
    public String getTaskType() { return TASK_TYPE; }

    @Override
    public boolean doWork() {
        SharedPreferences preferences = context.getSharedPreferences("cloudstore", MODE_PRIVATE);
        String cloudstoreUploadTimestampKey = preferences.getString("upload_cloudstore_key", "upload_timestamp");
        long cloudstoreUploadTimestamp = preferences.getLong(cloudstoreUploadTimestampKey, 0);

        Realm realmProcessedData = Realm.getInstance(realmConfigRawData);

        try {

            String collectionId = getCollectionId();
            if (collectionId == null) return false;

            //TODO research a way to reduce number of queries to Firebase CloudStore
//            FirebaseFirestore db = FirebaseFirestore.getInstance();

            //cloudstoreSynchronization.updateProgress(0, new Date(cloudstoreUploadTimestamp));

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
                dbDataItem.put("s", tagid);
                dbDataItem.put("d", base64data);
                dbDataItem.put("t", date);

//                Task addDocTask = db.collection(collectionId)
//                        .document()
//                        .set(dbDataItem);
//
//                Tasks.await(addDocTask);

                cloudstoreUploadTimestamp = date;

                float progress = (i+1) / (float) countAllNewRawData;
                Log.d(LOG_ID, "Uploaded until: " + new Date(cloudstoreUploadTimestamp) + ", progress: " + progress);
                //cloudstoreSynchronization.updateProgress(progress, new Date(cloudstoreUploadTimestamp));
            }

        } catch (Exception e) {
            Log.e(LOG_ID, "Error: " + e.toString());
            e.printStackTrace();
            return false;
        }
        finally {
            realmProcessedData.close();

            SharedPreferences.Editor preferencesEditor = preferences.edit();
            preferencesEditor.putLong(cloudstoreUploadTimestampKey, cloudstoreUploadTimestamp);
            preferencesEditor.apply();
        }
        return true;
    }

    @Override
    public Object getResult(){
        return null;
    }
}
