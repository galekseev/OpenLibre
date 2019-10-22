package com.camomile.openlibre.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.camomile.openlibre.OpenLibre;
import com.camomile.openlibre.model.db.UserProfile;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

public class CloudStoreSignInTask extends CloudStoreTask {

    private static final String LOG_ID = "OpenLibre::" + CloudStoreSignInTask.class.getSimpleName();
    public static final String TASK_TYPE = "SIGN_IN";

    private UserProfile mProfile;
    private String mDeviceAppToken;

    public CloudStoreSignInTask(final UserProfile profile, final String deviceAppToken, final ITaskContainer container){
        super(container);
        mProfile = profile;
        mDeviceAppToken = deviceAppToken;
    }

    public UserProfile getProfile() {
        return mProfile;
    }

    @Override
    public String getTaskType(){ return TASK_TYPE; }

    @Override
    public boolean doWork() {
        final DocumentReference docRef = OpenLibre.usersCollection.document(mProfile.getEmail());
        Task<DocumentSnapshot> taskGetProfile = docRef.get();

        try{
            DocumentSnapshot docSnapshot = Tasks.await(taskGetProfile);

            if (docSnapshot.exists()) {
                final UserProfile userProfile = docSnapshot.toObject(UserProfile.class);
                Task updateTask = null;
                if (userProfile.getType()== UserProfile.AccountType.LINKED) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    final DocumentReference docRefL = OpenLibre.usersCollection.document(userProfile.getMaster());
                    updateTask = db.runTransaction(new Transaction.Function<Void>() {
                        @Nullable
                        @Override
                        public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            transaction.update(docRef,
                                    "tokens", FieldValue.arrayUnion(mDeviceAppToken)
                            );
                            transaction.update(docRefL,
                                    "tokens", FieldValue.arrayUnion(mDeviceAppToken));
                            return null;
                        }
                    });
                }
                else {
                    updateTask = docRef.update("tokens", FieldValue.arrayUnion(mDeviceAppToken));
                }
                Tasks.await(updateTask);
                Log.d(LOG_ID, "updated tokens");
            }
            else {
                mProfile.getTokens().add(mDeviceAppToken);
                Task taskCreateProfile = docRef.set(mProfile);
                Tasks.await(taskCreateProfile);
            }

            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Object getResult(){
        return mProfile;
    }
}
