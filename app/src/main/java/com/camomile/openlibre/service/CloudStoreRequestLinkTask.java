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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;

public class CloudStoreRequestLinkTask extends CloudStoreTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreRequestLinkTask.class.getSimpleName();
    public static final String TASK_TYPE = "REQUEST_LINK";

    public static final int ERROR_USER_NOT_FOUND = 1;
    public static final int ERROR_LINK_SELF = 2;

    private String mMasterEmail;
    private String mSlaveEmail;

    public CloudStoreRequestLinkTask(final String masterEmail, final String slaveEmail, final ITaskContainer container){
        super(container);
        mMasterEmail = masterEmail;
        mSlaveEmail = slaveEmail;
    }

    @Override
    public String getTaskType(){ return TASK_TYPE; }

    @Override
    public boolean doWork() {
        final DocumentReference docRefM = OpenLibre.usersCollection.document(mMasterEmail);
        Task<DocumentSnapshot> getTask = docRefM.get();
        try {
            Log.v(LOG_ID, "Start sending link request");
            DocumentSnapshot docSnapshot = Tasks.await(getTask);
            if (docSnapshot.exists()){
                UserProfile profile = docSnapshot.toObject(UserProfile.class);
                if (!profile.getLinked().contains(mSlaveEmail) && !profile.getRequests().contains(mSlaveEmail))
                {
                    final DocumentReference docRefS = OpenLibre.usersCollection.document(mSlaveEmail);
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Task tr = db.runTransaction(new Transaction.Function<Void>() {
                        @Nullable
                        @Override
                        public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            transaction.update(docRefM, "requests", FieldValue.arrayUnion(mSlaveEmail));
                            transaction.update(docRefS,
                                    "master", mMasterEmail,
                                    "type", UserProfile.AccountType.REQUESTED.toString()
                            );
                            return null;
                        }
                    });

                    Tasks.await(tr);
                    Log.d(LOG_ID, String.format("Link request from %s to %s is successfully completed", mSlaveEmail, mMasterEmail));
                    return tr.isSuccessful();
                }
                Log.d(LOG_ID, String.format("Link request from %s to %s already registered", mSlaveEmail, mMasterEmail));
                return true;
            }
            else {
                Log.d(LOG_ID, String.format("User %s not found", mMasterEmail));
                errorCode = ERROR_USER_NOT_FOUND;
            }
        }
        catch (Exception e){
            errorCode = UNKNOWN_ERROR;
            Log.e(LOG_ID, e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Object getResult(){
        return mMasterEmail;
    }
}
