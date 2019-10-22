package com.camomile.openlibre.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.camomile.openlibre.OpenLibre;
import com.camomile.openlibre.model.db.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

public class CloudStoreDetachTask extends CloudStoreUnlinkTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreDetachTask.class.getSimpleName();
    // Detach account from master (initiated by master account)
    public static final String TASK_TYPE = "MASTER_DETACH";

    public CloudStoreDetachTask(final String masterEmail, final String linkedEmail, final ITaskContainer container){
        super(masterEmail, linkedEmail, container);
    }

    @Override
    public String getTaskType(){ return TASK_TYPE; }

    @Override
    public Object getResult(){
        return mLinkedEmail;
    }
}
