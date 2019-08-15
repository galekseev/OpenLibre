package de.dorianscholz.openlibre;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import java.io.File;
import java.util.ArrayList;

import de.dorianscholz.openlibre.model.ProcessedDataModule;
import de.dorianscholz.openlibre.model.RawDataModule;
import de.dorianscholz.openlibre.model.RawTagData;
import de.dorianscholz.openlibre.model.ReadingData;
import de.dorianscholz.openlibre.model.UserDataModule;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.Sort;

@ReportsCrashes(
        formUri = "http://www-stud.informatik.uni-frankfurt.de/~scholz/openlibre/report.php",
        reportType = HttpSender.Type.FORM,
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text,
        resDialogTitle = R.string.app_name,
        resDialogText = R.string.crash_dialog_text,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)
public class OpenLibre extends Application {

    private static final String LOG_ID = "OpenLibre::" + OpenLibre.class.getSimpleName();

    // settings_
    public static boolean NFC_USE_MULTI_BLOCK_READ = true;
    public static boolean GLUCOSE_UNIT_IS_MMOL = false;
    public static float GLUCOSE_TARGET_MIN = 80;
    public static float GLUCOSE_TARGET_MAX = 140;

    public static RealmConfiguration realmConfigRawData;
    public static RealmConfiguration realmConfigProcessedData;
    public static RealmConfiguration realmConfigUserData;
    public static File openLibreDataPath;


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // if it is the onCreate for ARCA, skip own init tasks
        if (ACRA.isACRASenderServiceProcess()) {
            return;
        }

        refreshApplicationSettings(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        Realm.init(this);

        setupRealm(getApplicationContext());

        parseRawData();

        StethoUtils.install(this, openLibreDataPath);
    }

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

    static void parseRawData() {
        Realm realmRawData = Realm.getInstance(realmConfigRawData);
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);

        // if processed data realm is empty
        if (realmProcessedData.isEmpty() && !realmRawData.isEmpty()) {
            // parse data from raw realm into processed data realm
            Log.i(LOG_ID, "Parsing data raw_data realm to processed_data realm.");
            realmProcessedData.beginTransaction();
            for (RawTagData rawTagData : realmRawData.where(RawTagData.class)
                            .findAllSorted(RawTagData.DATE, Sort.ASCENDING)) {
                realmProcessedData.copyToRealmOrUpdate(new ReadingData(rawTagData));
            }
            realmProcessedData.commitTransaction();
        }

        realmProcessedData.close();
        realmRawData.close();
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
