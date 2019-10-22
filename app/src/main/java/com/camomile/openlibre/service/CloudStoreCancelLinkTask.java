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

public class CloudStoreCancelLinkTask extends CloudStoreTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreCancelLinkTask.class.getSimpleName();
    public static final String TASK_TYPE = "CANCEL_LINK";

    public static final int ERROR_LINK_APPROVED = 3;
    public static final int ERROR_USER_NOT_FOUND = 1;

    private String mMasterEmail;
    private String mSlaveEmail;

    public CloudStoreCancelLinkTask(final String masterEmail, final String slaveEmail, final ITaskContainer container){
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
            Log.v(LOG_ID, "Start sending cancel link request");
            DocumentSnapshot docSnapshot = Tasks.await(getTask);
            if (docSnapshot.exists()){
                UserProfile profile = docSnapshot.toObject(UserProfile.class);

                if (profile.getLinked().contains(mSlaveEmail)) {
                    Log.v(LOG_ID, String.format("Link request from %s to %s is already approved", mSlaveEmail, mMasterEmail));
                    errorCode = ERROR_LINK_APPROVED;
                    return false;
                }

                final DocumentReference docRefS = OpenLibre.usersCollection.document(mSlaveEmail);
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Task updateTask = db.runTransaction(new Transaction.Function<Void>() {
                    @Nullable
                    @Override
                    public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                        transaction.update(docRefM, "requests", FieldValue.arrayRemove(mSlaveEmail));
                        transaction.update(docRefS,
                                "master", null,
                                "type", UserProfile.AccountType.MASTER.toString()
                        );
                        return null;
                    }
                });

                Tasks.await(updateTask);
                Log.d(LOG_ID, String.format("Cancel link request from %s to %s is successfully completed", mSlaveEmail, mMasterEmail));
                return updateTask.isSuccessful();
            }
            else {
                Log.d(LOG_ID, String.format("User %s not found", mMasterEmail));
                errorCode = ERROR_USER_NOT_FOUND;
            }
        }
        catch (Exception e){
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
