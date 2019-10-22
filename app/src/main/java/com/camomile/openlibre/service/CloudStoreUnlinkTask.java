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

public class CloudStoreUnlinkTask extends CloudStoreTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreUnlinkTask.class.getSimpleName();
    //Unlink account from master (initiated by linked account)
    public static final String TASK_TYPE = "LINKED_UNLINK";

    public static final int ERROR_USER_NOT_FOUND = 1;

    protected String mMasterEmail;
    protected String mLinkedEmail;

    public CloudStoreUnlinkTask(final String masterEmail, final String slaveEmail, final ITaskContainer container){
        super(container);
        mMasterEmail = masterEmail;
        mLinkedEmail = slaveEmail;
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
                if (profile.getLinked().contains(mLinkedEmail))
                {
                    final DocumentReference docRefL = OpenLibre.usersCollection.document(mLinkedEmail);
                    DocumentSnapshot docLSnapshot = Tasks.await(docRefL.get());
                    final UserProfile lProfile = docLSnapshot.toObject(UserProfile.class);
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Task tr = db.runTransaction(new Transaction.Function<Void>() {
                        @Nullable
                        @Override
                        public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            transaction.update(docRefM,
                                    "linked", FieldValue.arrayRemove(mLinkedEmail),
                                    "tokens", FieldValue.arrayRemove(lProfile.getTokens().toArray())
                            );
                            transaction.update(docRefL,
                                    "type", UserProfile.AccountType.MASTER.toString(),
                                    "masterUid", null
                            );
                            return null;
                        }
                    });

                    Tasks.await(tr);
                    Log.d(LOG_ID, String.format("Link from %s to %s is successfully removed", mLinkedEmail, mMasterEmail));
                    return tr.isSuccessful();
                }
                Log.d(LOG_ID, String.format("User %s and %s aren't linked", mLinkedEmail, mMasterEmail));
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
        return new String[]{ mMasterEmail };
    }
}
