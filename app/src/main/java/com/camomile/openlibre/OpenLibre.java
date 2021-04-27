package com.camomile.openlibre;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.camomile.openlibre.model.ProcessedDataModule;
import com.camomile.openlibre.model.RawDataModule;
import com.camomile.openlibre.model.RawTagData;
import com.camomile.openlibre.model.ReadingData;
import com.camomile.openlibre.model.UserDataModule;

import java.io.File;
import java.util.ArrayList;

import com.camomile.openlibre.model.db.UserProfile;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.Sort;

public class OpenLibre extends Application {

    private static final String LOG_ID = "OpenLibre::" + OpenLibre.class.getSimpleName();

    // settings_
    public static boolean NFC_USE_MULTI_BLOCK_READ = true;
    public static boolean GLUCOSE_UNIT_IS_MMOL = false;
    public static float GLUCOSE_TARGET_MIN = 80;
    public static float GLUCOSE_TARGET_MAX = 140;

    // realm db
    public static RealmConfiguration realmConfigRawData;
    public static RealmConfiguration realmConfigProcessedData;
    public static RealmConfiguration realmConfigUserData;
    public static File openLibreDataPath;

    // Http request queue
    public static RequestQueue volleyRequestQueue;

    // auth
    public static String deviceAppToken;

//    // Firestore
//    public static FirebaseFirestore firestore = null;
//    public static CollectionReference usersCollection = null;

    //User profile
    public static UserProfile userProfile = null;

    @Override
    public void onCreate() {
        super.onCreate();

        refreshApplicationSettings(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        Realm.init(this);

        setupRealm(getApplicationContext());

        parseRawData();

//        firestore = FirebaseFirestore.getInstance();
//        usersCollection = firestore.collection("users");

//        setupAccount();

//        int apisAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
//        boolean av = apisAvailable == ConnectionResult.SUCCESS;
//        Log.d(LOG_ID, "Google Apis available: " + Boolean.toString(av));
        volleyRequestQueue = Volley.newRequestQueue(this.getApplicationContext());

//        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(
//                new OnCompleteListener<InstanceIdResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
//                        if (!task.isSuccessful()) {
//                            Log.w(LOG_ID, "getInstanceId failed", task.getException());
//                            return;
//                        }
//
//                        // Get new Instance ID token
//                        deviceAppToken = task.getResult().getToken();
//                        Log.d(LOG_ID, "app token: " + deviceAppToken);
//                    }
//                }
//        );

//        StethoUtils.install(this, openLibreDataPath);
    }

//    private void setupAccount() {
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//
//        if (user != null) {
//            usersCollection.document(user.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                @Override
//                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()){
//                        userProfile = task.getResult().toObject(UserProfile.class);
//                        if (userProfile.getType() == UserProfile.AccountType.LINKED){
//                            usersCollection.document(userProfile.getMaster()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                                @Override
//                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()){
//                                        UserProfile master = task.getResult().toObject(UserProfile.class);
//                                        userProfile.setTokens(master.getTokens());
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    e.printStackTrace();
//                }
//            });
//        }
//    }

    public static void refreshApplicationSettings(SharedPreferences settings) {
        // read settings values
        NFC_USE_MULTI_BLOCK_READ = settings.getBoolean("pref_nfc_use_multi_block_read", NFC_USE_MULTI_BLOCK_READ);
        GLUCOSE_UNIT_IS_MMOL = settings.getBoolean("pref_glucose_unit_is_mmol", GLUCOSE_UNIT_IS_MMOL);
        GLUCOSE_TARGET_MIN = Float.parseFloat(settings.getString("pref_glucose_target_min", Float.toString(GLUCOSE_TARGET_MIN)));
        GLUCOSE_TARGET_MAX = Float.parseFloat(settings.getString("pref_glucose_target_max", Float.toString(GLUCOSE_TARGET_MAX)));
    }

    public static void setupRealm(Context context) {
        // get data path from settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String dataPathName = settings.getString("open_libre_data_path", null);

        if (dataPathName != null) {
            openLibreDataPath = new File(dataPathName);
            Log.i(LOG_ID, "Using saved data path: '" + openLibreDataPath.toString() + "'");
        } else {
            final ArrayList<String> dataPathNames = new ArrayList<>();
            dataPathNames.add(new File(Environment.getExternalStorageDirectory().getPath(), "openlibre").toString());
            dataPathNames.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString());
            dataPathNames.add(context.getFilesDir().toString());

            // if data path is not saved in settings, search for existing realms
            for (String pathName : dataPathNames) {
                if (new File(pathName, "data_raw.realm").exists()) {
                    openLibreDataPath = new File(pathName);
                    Log.i(LOG_ID, "Using existing data path: '" + openLibreDataPath.toString() + "'");
                    break;
                }
            }

            // if no existing realm was found, find a storage path that we can actually create a realm in
            if (openLibreDataPath == null) {
                for (String pathName : dataPathNames) {
                    if (tryRealmStorage(new File(pathName))) {
                        openLibreDataPath = new File(pathName);
                        Log.i(LOG_ID, "Using new data path: '" + openLibreDataPath.toString() + "'");
                        break;
                    }
                }
            }

            // save data path
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("open_libre_data_path", openLibreDataPath.toString());
            editor.apply();
        }

        realmConfigUserData = new RealmConfiguration.Builder()
                .modules(new UserDataModule())
                .directory(openLibreDataPath)
                .name("data_user.realm")
                .schemaVersion(2)
                .migration(new UserDataRealmMigration())
                .build();


        realmConfigRawData = new RealmConfiguration.Builder()
                .modules(new RawDataModule())
                .directory(openLibreDataPath)
                .name("data_raw.realm")
                .schemaVersion(3)
                .migration(new RawDataRealmMigration())
                .build();

        realmConfigProcessedData = new RealmConfiguration.Builder()
                .modules(new ProcessedDataModule())
                .directory(openLibreDataPath)
                .name("data_processed.realm")
                .schemaVersion(2)
                // delete processed data realm, if data structure changed
                // it will just be parsed again from the raw data
                .deleteRealmIfMigrationNeeded()
                .build();
    }

    public static void parseRawData() {
        Realm realmRawData = Realm.getInstance(realmConfigRawData);
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);

        // if processed data realm is empty
        if (realmProcessedData.isEmpty() && !realmRawData.isEmpty()) {
            // parse data from raw realm into processed data realm
            Log.i(LOG_ID, "Parsing data raw_data realm to processed_data realm.");
            realmProcessedData.beginTransaction();
            for (RawTagData rawTagData : realmRawData.where(RawTagData.class)
                            .sort(RawTagData.DATE, Sort.ASCENDING).findAll()) {
                realmProcessedData.copyToRealmOrUpdate(new ReadingData(rawTagData));
            }
            realmProcessedData.commitTransaction();
        }

        realmProcessedData.close();
        realmRawData.close();
    }

    public static void clearRealmData(Context application){
        Realm realmRawData = Realm.getInstance(realmConfigRawData);
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);

        realmRawData.beginTransaction();
        realmRawData.deleteAll();
        realmRawData.commitTransaction();

        realmProcessedData.beginTransaction();
        realmProcessedData.deleteAll();
        realmProcessedData.commitTransaction();

        SharedPreferences preferences = application.getSharedPreferences("cloudstore", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        String cloudstoreDownloadTimestampKey = preferences.getString("download_cloudstore_key", "download_timestamp");
        editor.putLong(cloudstoreDownloadTimestampKey, 0);
        editor.apply();
    }

    private static boolean tryRealmStorage(File path) {
        // check where we can actually store the databases on this device
        RealmConfiguration realmTestConfiguration;

        // catch all errors when creating directory and db
        try {
            realmTestConfiguration = new RealmConfiguration.Builder()
                    .directory(path)
                    .name("test_storage.realm")
                    .deleteRealmIfMigrationNeeded()
                    .build();
            Realm testInstance = Realm.getInstance(realmTestConfiguration);
            testInstance.close();
            Realm.deleteRealm(realmTestConfiguration);
        } catch (Throwable e) {
            Log.i(LOG_ID, "Test creation of realm failed for: '" + path.toString() + "': " + e.toString());
            return false;
        }

        return true;
    }

}
