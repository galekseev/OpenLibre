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

public class CloudStoreDenyLinkTask extends CloudStoreTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreDenyLinkTask.class.getSimpleName();
    public static final String TASK_TYPE = "DENY_LINK";

    public static final int ERROR_USER_NOT_FOUND = 1;

    private String mMasterEmail;
    private String mLinkedEmail;

    public CloudStoreDenyLinkTask(final String masterEmail, final String linkedEmail, final ITaskContainer container){
        super(container);
        mMasterEmail = masterEmail;
        mLinkedEmail = linkedEmail;
    }

    @Override
    public String getTaskType(){ return TASK_TYPE; }

    @Override
    public boolean doWork() {
        final DocumentReference docRefM = OpenLibre.usersCollection.document(mMasterEmail);
        Task<DocumentSnapshot> getTask = docRefM.get();
        try {
            DocumentSnapshot docSnapshot = Tasks.await(getTask);
            if (docSnapshot.exists()){
                UserProfile profile = docSnapshot.toObject(UserProfile.class);
                if (!profile.getLinked().contains(mLinkedEmail) && profile.getRequests().contains(mLinkedEmail))
                {
                    final DocumentReference docRefS = OpenLibre.usersCollection.document(mLinkedEmail);
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Task tr = db.runTransaction(new Transaction.Function<Void>() {
                        @Nullable
                        @Override
                        public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            transaction.update(docRefM,
                                    "requests", FieldValue.arrayRemove(mLinkedEmail)
                            );
                            transaction.update(docRefS,
                                    "type", UserProfile.AccountType.DENIED.toString()
                            );
                            return null;
                        }
                    });

                    Tasks.await(tr);
                    Log.d(LOG_ID, String.format("Link from %s to %s is successfully allowed", mLinkedEmail, mMasterEmail));
                    return tr.isSuccessful();
                }
                Log.d(LOG_ID, String.format("Link request from %s to %s already registered", mLinkedEmail, mMasterEmail));
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
        return mLinkedEmail;
    }
}
