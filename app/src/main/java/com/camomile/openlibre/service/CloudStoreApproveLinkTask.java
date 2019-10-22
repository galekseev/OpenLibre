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

public class CloudStoreApproveLinkTask extends CloudStoreTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreApproveLinkTask.class.getSimpleName();
    public static final String TASK_TYPE = "ALLOW_LINK";

    public static final int ERROR_USER_NOT_FOUND = 1;

    private String mMasterEmail;
    private String mLinkedEmail;
    private String mMasterUid;

    public CloudStoreApproveLinkTask(final String masterEmail, final String masterUid, final String linkedEmail, final ITaskContainer container){
        super(container);
        mMasterEmail = masterEmail;
        mMasterUid = masterUid;
        mLinkedEmail = linkedEmail;
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
                UserProfile mProfile = docSnapshot.toObject(UserProfile.class);
                if (!mProfile.getLinked().contains(mLinkedEmail) && mProfile.getRequests().contains(mLinkedEmail))
                {
                    final DocumentReference docRefL = OpenLibre.usersCollection.document(mLinkedEmail);
                    DocumentSnapshot docLSnapshot = Tasks.await(docRefL.get());
                    final UserProfile lProfile = docLSnapshot.toObject(UserProfile.class);
                    final String[] tokens = lProfile.getTokens().toArray(new String[lProfile.getTokens().size()]);
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Task tr = db.runTransaction(new Transaction.Function<Void>() {
                        @Nullable
                        @Override
                        public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            transaction.update(docRefM,
                                    "requests", FieldValue.arrayRemove(mLinkedEmail),
                                    "linked", FieldValue.arrayUnion(mLinkedEmail),
                                    "tokens", FieldValue.arrayUnion(tokens)
                            );
                            transaction.update(docRefL,
                                    "master", mMasterEmail,
                                    "masterUid", mMasterUid,
                                    "type", UserProfile.AccountType.LINKED.toString()
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
