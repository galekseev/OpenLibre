package com.camomile.openlibre.service;

import android.content.Context;
import android.media.AudioManager;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.camomile.openlibre.OpenLibre;
import com.camomile.openlibre.R;
import com.camomile.openlibre.model.RawTagData;
import com.camomile.openlibre.model.ReadingData;
import com.camomile.openlibre.ui.MainActivity;
import io.realm.Realm;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.media.AudioManager.RINGER_MODE_SILENT;
import static com.camomile.openlibre.OpenLibre.realmConfigProcessedData;
import static com.camomile.openlibre.OpenLibre.realmConfigRawData;
import static com.camomile.openlibre.model.AlgorithmUtil.bytesToHexString;


public class NfcVReaderTask extends AsyncTask<Tag, Void, Boolean> {
    private static final String LOG_ID = "OpenLibre::" + NfcVReaderTask.class.getSimpleName();
    public static final long[] vibrationPatternSuccess = {0, 200, 100, 200}; // [ms]
    public static final long[] vibrationPatternFailure = {0, 500}; // [ms]
    private static final long nfcReadTimeout = 1000; // [ms]

    private MainActivity mainActivity;
    private String sensorTagId;
    private byte[] data;

    public NfcVReaderTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        data = new byte[360];
    }

    @Override
    protected void onPostExecute(Boolean success) {
        mainActivity.findViewById(R.id.pb_scan_circle).setVisibility(View.INVISIBLE);

        Vibrator vibrator = (Vibrator) mainActivity.getSystemService(VIBRATOR_SERVICE);
        AudioManager audioManager = (AudioManager) mainActivity.getSystemService(Context.AUDIO_SERVICE);

        if (!success) {
            Toast.makeText(mainActivity,
                    mainActivity.getResources().getString(R.string.reading_sensor_error),
                    Toast.LENGTH_SHORT
            ).show();

            if (audioManager.getRingerMode() != RINGER_MODE_SILENT) {
                vibrator.vibrate(vibrationPatternFailure, -1);
            }
            return;
        }

        if (audioManager.getRingerMode() != RINGER_MODE_SILENT) {
            vibrator.vibrate(vibrationPatternSuccess, -1);
        }

        if (RawTagData.getSensorReadyInMinutes(data) > 0) {
            Toast.makeText(mainActivity,
                    mainActivity.getResources().getString(R.string.reading_sensor_not_ready) + " " +
                            String.format(mainActivity.getResources().getString(R.string.sensor_ready_in), RawTagData.getSensorReadyInMinutes(data)) + " " +
                            mainActivity.getResources().getString(R.string.minutes),
                    Toast.LENGTH_LONG
            ).show();

            final TextView tv_sensor_ready_counter = (TextView) mainActivity.findViewById(R.id.tv_sensor_ready_counter);
            tv_sensor_ready_counter.setVisibility(View.VISIBLE);

            new CountDownTimer(TimeUnit.MINUTES.toMillis(RawTagData.getSensorReadyInMinutes(data)), TimeUnit.MINUTES.toMillis(1)) {
                public void onTick(long millisUntilFinished) {
                    int readyInMinutes = (int) Math.ceil(((double) millisUntilFinished) / TimeUnit.MINUTES.toMillis(1));
                    tv_sensor_ready_counter.setText(String.format(
                            mainActivity.getResources().getString(R.string.sensor_ready_in),
                            readyInMinutes));
                }

                public void onFinish() {
                    tv_sensor_ready_counter.setVisibility(View.INVISIBLE);
                }
            }.start();

            return;
        }

        Toast.makeText(mainActivity,
                mainActivity.getResources().getString(R.string.reading_sensor_success),
                Toast.LENGTH_SHORT
        ).show();

        // FIXME: the new data should be propagated transparently through the database backend
        mainActivity.onNfcReadingFinished(processRawData(sensorTagId, data));
    }

    @Override
    protected Boolean doInBackground(Tag... params) {
        Tag tag = params[0];
        sensorTagId = bytesToHexString(tag.getId());
        return readNfcTag(tag);
    }

    private boolean readNfcTag(Tag tag) {
        updateProgressBar(0);
        NfcV nfcvTag = NfcV.get(tag);
        Log.d(NfcVReaderTask.LOG_ID, "Attempting to read tag data");
        try {
            nfcvTag.connect();
            final byte[] uid = tag.getId();
            final int step = OpenLibre.NFC_USE_MULTI_BLOCK_READ ? 3 : 1;
            final int blockSize = 8;

            for (int blockIndex = 0; blockIndex <= 40; blockIndex += step) {
                byte[] cmd;
                if (OpenLibre.NFC_USE_MULTI_BLOCK_READ) {
                    cmd = new byte[]{0x02, 0x23, (byte) blockIndex, 0x02}; // multi-block read 3 blocks
                } else {
                    cmd = new byte[]{0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, (byte) blockIndex, 0};
                    System.arraycopy(uid, 0, cmd, 2, 8);
                }

                byte[] readData;
                Long startReadingTime = System.currentTimeMillis();
                while (true) {
                    try {
                        readData = nfcvTag.transceive(cmd);
                        break;
                    } catch (IOException e) {
                        if ((System.currentTimeMillis() > startReadingTime + nfcReadTimeout)) {
                            Log.e(NfcVReaderTask.LOG_ID, "tag read timeout");
                            return false;
                        }
                    }
                }

                if (OpenLibre.NFC_USE_MULTI_BLOCK_READ) {
                    System.arraycopy(readData, 1, data, blockIndex * blockSize, readData.length - 1);
                } else {
                    readData = Arrays.copyOfRange(readData, 2, readData.length);
                    System.arraycopy(readData, 0, data, blockIndex * blockSize, blockSize);
                }

                updateProgressBar(blockIndex);
            }
            Log.d(NfcVReaderTask.LOG_ID, "Got NFC tag data");

        } catch (Exception e) {

            Log.i(NfcVReaderTask.LOG_ID, e.toString());
            return false;

        } finally {
            try {
                nfcvTag.close();
            } catch (Exception e) {
                Log.e(NfcVReaderTask.LOG_ID, "Error closing tag!");
            }
        }
        Log.d(NfcVReaderTask.LOG_ID, "Tag data reader exiting");
        return true;
    }

    private void updateProgressBar(int blockIndex) {
        final int progress = blockIndex;
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                ((ProgressBar) mainActivity.findViewById(R.id.pb_scan_circle)).setProgress(progress);
            }
        });
    }

    // to be able to use the returned ReadingData object, this has to be called from the GUI thread
    public static ReadingData processRawData(String sensorTagId, byte[] data) {
        // copy data to database
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);
        Realm realmRawData = Realm.getInstance(realmConfigRawData);

        // commit raw data into realm for debugging
        realmRawData.beginTransaction();
        RawTagData rawTagData = realmRawData.copyToRealmOrUpdate(new RawTagData(sensorTagId, data));
        realmRawData.commitTransaction();

        // commit processed data into realm
        realmProcessedData.beginTransaction();
        ReadingData readingData = realmProcessedData.copyToRealmOrUpdate(new ReadingData(rawTagData));
        realmProcessedData.commitTransaction();

        realmProcessedData.close();
        realmRawData.close();

        return readingData;
    }

    public static void processRawDataList(List<RawTagData> rawTagDataList){
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);
        Realm realmRawData = Realm.getInstance(realmConfigRawData);

        realmRawData.beginTransaction();
        List<RawTagData> realmRawTagDataList = new ArrayList<RawTagData>();
        for(RawTagData rawTagData: rawTagDataList){
            RawTagData realmRawTagData = realmRawData.copyToRealmOrUpdate(rawTagData);
            realmRawTagDataList.add(realmRawTagData);
        }
        realmRawData.commitTransaction();

        realmProcessedData.beginTransaction();
        for(RawTagData rawTagData: realmRawTagDataList){
            realmProcessedData.copyToRealmOrUpdate(new ReadingData(rawTagData));
        }
        realmProcessedData.commitTransaction();

        realmProcessedData.close();
        realmRawData.close();
    }
}
