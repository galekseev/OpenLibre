package com.camomile.openlibre.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

//import com.camomile.openlibre.BuildConfig;
import com.camomile.openlibre.model.RawTagData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

class CloudStoreDownloadDataTask extends CloudStoreDataTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreDownloadDataTask.class.getSimpleName();
    public static final String TASK_TYPE = "DOWNLOAD_NEW_DATA";

    CloudStoreDownloadDataTask(Context context, CloudStoreSynchronization cloudstoreSynchronization) {
        super(context, cloudstoreSynchronization);
    }

    @Override
    public String getTaskType(){ return TASK_TYPE; }

    @Override
    public boolean doWork() {

        SharedPreferences preferences = context.getSharedPreferences("cloudstore", MODE_PRIVATE);
        String cloudstoreDownloadTimestampKey = preferences.getString("download_cloudstore_key", "download_timestamp");
        long cloudstoreDownloadTimestamp = preferences.getLong(cloudstoreDownloadTimestampKey, 0);

        //cloudstoreDownloadTimestamp = 0;
        //if (BuildConfig.DEBUG) cloudstoreDownloadTimestamp = 0;

        String collectionId = getCollectionId();

        if (collectionId == null) return false;

        //TODO research a way to reduce number of queries to Firebase CloudStore
        try {
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//            Log.d(LOG_ID,"Start quering data");
//            Task<QuerySnapshot> task = db.collection(collectionId)
//                    .whereGreaterThan("t", cloudstoreDownloadTimestamp)
//                    .orderBy("t", Query.Direction.ASCENDING)
//                    .limit(1000)
//                    .get();
//
//            Tasks.await(task);

            Log.v(LOG_ID, "Query completed");
//            if (task.isComplete() && task.isSuccessful()){
//
//                QuerySnapshot querySnapshot = task.getResult();
//                if (querySnapshot.isEmpty()){
//                    Log.d(LOG_ID, "Empty result set returned");
//                    return true;
//                }
//
//                List<RawTagData> rawTagDataList = new ArrayList<RawTagData>();
//                for (QueryDocumentSnapshot document: querySnapshot){
//                    String sensor = document.getString("s");
//                    long utc_date = document.getLong("t");
//                    String dataString = document.getString("d");
//                    byte[] data = Base64.decode(dataString, Base64.DEFAULT);
//                    Log.v(LOG_ID, String.format("Reading data: t=%s ; s=%s", utc_date, sensor));
//                    rawTagDataList.add(new RawTagData(sensor, utc_date, data));
//                    cloudstoreDownloadTimestamp = utc_date;
//                }
//
//                Log.d(LOG_ID, String.format("Read %s tags", rawTagDataList.size()));
//                NfcVReaderTask.processRawDataList(rawTagDataList);
//
//                //cloudstoreSynchronization.updateProgress(1, new Date(cloudstoreDownloadTimestamp));
//            }
        }
        catch (Exception e) {
            Log.e(LOG_ID, "Error: " + e.toString());
            e.printStackTrace();
        }
        finally {
            SharedPreferences.Editor preferencesEditor = preferences.edit();
            preferencesEditor.putLong(cloudstoreDownloadTimestampKey, cloudstoreDownloadTimestamp);
            preferencesEditor.apply();
        }

        return true;
    }
}