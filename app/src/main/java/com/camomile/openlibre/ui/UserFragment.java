package com.camomile.openlibre.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.camomile.openlibre.OpenLibre;
import com.camomile.openlibre.R;
import com.camomile.openlibre.model.db.UserProfile;
import com.camomile.openlibre.service.CloudStoreApproveLinkTask;
import com.camomile.openlibre.service.CloudStoreCancelLinkTask;
import com.camomile.openlibre.service.CloudStoreDenyLinkTask;
import com.camomile.openlibre.service.CloudStoreDetachTask;
import com.camomile.openlibre.service.CloudStoreRequestLinkTask;
import com.camomile.openlibre.service.CloudStoreSignInTask;
import com.camomile.openlibre.service.CloudStoreTask;
import com.camomile.openlibre.service.CloudStoreUnlinkTask;
import com.camomile.openlibre.service.ITaskContainer;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class UserFragment extends Fragment implements ITaskContainer
{
    private static final String LOG_ID = "OpenLibre::" + UserFragment.class.getSimpleName();

    private TextView mLabelUser;
    private AppCompatButton mSignInButton;
    private AppCompatButton mLogoutButton;
    private AppCompatButton mLinkButton;
    private AppCompatButton mUnlinkButton;
    private AppCompatButton mSyncStatus;
    private AppCompatButton mCancelLinkRequest;
    private RelativeLayout rlProgressBar;
    private RecyclerView listInvites;

    public static UserFragment newInstance() {
        return new UserFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        Context context = view.getContext();
        listInvites = view.findViewById(R.id.user_invites_recycle_view);
        listInvites.setLayoutManager(new LinearLayoutManager(context));

        listInvites.setAdapter(new UserLinksRecyclerViewAdapter(
                this,
                OpenLibre.userProfile
        ));

        listInvites.setHasFixedSize(true);
        listInvites.addItemDecoration(
                new DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL_LIST)
        );
        registerForContextMenu(listInvites);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        final FirebaseUser user = auth.getCurrentUser();

        //region Buttons setup
        mLabelUser = view.findViewById(R.id.label_user);
        mSignInButton = view.findViewById(R.id.button_sign_in);
        mLogoutButton = view.findViewById(R.id.button_log_out);

        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSignInActivity(view);
            }
        });
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut(auth);
            }
        });

        //Link account buttons
        mLinkButton = view.findViewById(R.id.button_link);
        mUnlinkButton = view.findViewById(R.id.button_unlink);
        mSyncStatus = view.findViewById(R.id.button_sync_link_request);
        mCancelLinkRequest = view.findViewById(R.id.button_cancel_link_request);

        mLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildEmailDialog().show();
            }
        });
        mUnlinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unlinkLinked(OpenLibre.userProfile.getMaster());
            }
        });
        mSyncStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                syncStatus(view);
            }
        });
        mCancelLinkRequest.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                cancelLink(OpenLibre.userProfile.getMaster());
            }
        });
        //endregion

        rlProgressBar = view.findViewById(R.id.rl_progress_bar);
        rlProgressBar.setVisibility(View.INVISIBLE);

        updateUI(user);
    }

    //region Linking methods
    protected void link(String email) {
        String slaveEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        rlProgressBar.setVisibility(View.VISIBLE);
        final CloudStoreRequestLinkTask linkTask = new CloudStoreRequestLinkTask(email, slaveEmail, this);
        linkTask.execute();
    }

    protected void cancelLink(String masterEmail) {
        String linkEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        rlProgressBar.setVisibility(View.VISIBLE);
        CloudStoreCancelLinkTask linkTask = new CloudStoreCancelLinkTask(masterEmail, linkEmail, this);
        linkTask.execute();
    }

    private void syncStatus(View view) {
        OpenLibre.userProfile.setType(UserProfile.AccountType.LINKED);
        updateUI(FirebaseAuth.getInstance().getCurrentUser());
    }

    protected void unlinkLinked(String masterEmail){
        String linkedEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        rlProgressBar.setVisibility(View.VISIBLE);
        CloudStoreUnlinkTask linkTask = new CloudStoreUnlinkTask(masterEmail, linkedEmail, this);
        linkTask.execute();
    }

    protected void detachFromMaster(String linkedEmail){
        String masterEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        rlProgressBar.setVisibility(View.VISIBLE);
        CloudStoreDetachTask linkTask = new CloudStoreDetachTask(masterEmail, linkedEmail, this);
        linkTask.execute();
    }

    protected void approveLink(String linkedEmail){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String masterEmail = currentUser.getEmail();
        String masterUid = currentUser.getUid();
        rlProgressBar.setVisibility(View.VISIBLE);
        CloudStoreApproveLinkTask linkTask = new CloudStoreApproveLinkTask(masterEmail, masterUid, linkedEmail, this);
        linkTask.execute();
    }

    protected void denyLink(String linkedEmail) {
        String masterEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        rlProgressBar.setVisibility(View.VISIBLE);
        CloudStoreDenyLinkTask linkTask = new CloudStoreDenyLinkTask(masterEmail, linkedEmail, this);
        linkTask.execute();
    }
    //endregion

    private AlertDialog buildEmailDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter account to link");

// Set up the input
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = input.getText().toString();
                link(email);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    private void signOut(FirebaseAuth auth){
        AuthUI.getInstance()
            .signOut(getActivity().getApplicationContext())
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                 @Override
                 public void onComplete(@NonNull Task<Void> task) {
                     OpenLibre.userProfile = null;
                     OpenLibre.clearRealmData(getContext());
                     Log.d(LOG_ID, "User signed out");
                 }
             });
        updateUI(null);
        listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, null));
    }

    private void updateUI(FirebaseUser user)
    {
        rlProgressBar.setVisibility(View.INVISIBLE);

        if (user == null || OpenLibre.userProfile == null) {
            mLabelUser.setText(R.string.please_login);
            mSignInButton.setVisibility(View.VISIBLE);
            mLogoutButton.setVisibility(View.GONE);
            mLinkButton.setVisibility(View.GONE);
            mUnlinkButton.setVisibility(View.GONE);
            mSyncStatus.setVisibility(View.GONE);
            mCancelLinkRequest.setVisibility(View.GONE);
        }
        else {

            String labelUserText = String.format("Name: %s\nID: %s", user.getEmail(), user.getUid());
            switch (OpenLibre.userProfile.getType())
            {
                case LINKED:
                    mLinkButton.setVisibility(View.GONE);
                    mUnlinkButton.setVisibility(View.VISIBLE);
                    mCancelLinkRequest.setVisibility(View.GONE);
                    mSyncStatus.setVisibility(View.GONE);
                    labelUserText += String.format("\nLinked to email: %s \nID: %s", OpenLibre.userProfile.getMaster(), OpenLibre.userProfile.getMasterUid());
                    break;
                case DENIED:
                    labelUserText += String.format("\nLink to %s denied", OpenLibre.userProfile.getMaster());
                case MASTER:
                    boolean noLinked = OpenLibre.userProfile.getLinked().size() == 0;
                    mLinkButton.setVisibility(noLinked ? View.VISIBLE : View.GONE);
                    mUnlinkButton.setVisibility(View.GONE);
                    mCancelLinkRequest.setVisibility(View.GONE);
                    mSyncStatus.setVisibility(View.GONE);
                    break;
                case REQUESTED:
                    mLinkButton.setVisibility(View.GONE);
                    mUnlinkButton.setVisibility(View.GONE);
                    mCancelLinkRequest.setVisibility(View.VISIBLE);
                    mSyncStatus.setVisibility(View.VISIBLE);
                    labelUserText += String.format("\nRequested link to %s", OpenLibre.userProfile.getMaster());
                    break;
            }

            mLabelUser.setText(labelUserText);
            mSignInButton.setVisibility(View.GONE);
            mLogoutButton.setVisibility(View.VISIBLE);
        }
    }

    //region SignIn
    public void startSignInActivity(View view)
    {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO Handle sign-in error
        if (requestCode == 0) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Log.d(LOG_ID, "Successful sign in. Email:" + user.getEmail() + " User:" + user.getDisplayName() + " ID:" + user.getUid());

                UserProfile profile = new UserProfile(user.getEmail(), user.getUid());

                CloudStoreSignInTask signInTask = new CloudStoreSignInTask(profile, OpenLibre.deviceAppToken, this);
                signInTask.execute();
                //CloudStoreUserTask.signIn(user.getEmail(), user.getUid(), OpenLibre.deviceAppToken);
                //updateUI(user);
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Log.d(LOG_ID, "Sign in failed");
            }
        }
    }
    //endregion

    public void onTaskSuccess(CloudStoreTask task) {

        switch (task.getTaskType()){
            case CloudStoreSignInTask.TASK_TYPE:
                OpenLibre.userProfile = (UserProfile) task.getResult();
                listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, OpenLibre.userProfile));
                break;
            case CloudStoreRequestLinkTask.TASK_TYPE:
                OpenLibre.userProfile.setMaster((String) task.getResult());
                OpenLibre.userProfile.setType(UserProfile.AccountType.REQUESTED);
                listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, null));
                break;
            case CloudStoreCancelLinkTask.TASK_TYPE:
                OpenLibre.userProfile.setMaster(null);
                OpenLibre.userProfile.setType(UserProfile.AccountType.MASTER);
                listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, OpenLibre.userProfile));
                break;
            case CloudStoreApproveLinkTask.TASK_TYPE:
                String linkedEmail = (String) task.getResult();
                OpenLibre.userProfile.getRequests().remove(linkedEmail);
                OpenLibre.userProfile.getLinked().add(linkedEmail);
                listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, OpenLibre.userProfile));
                break;
            case CloudStoreDenyLinkTask.TASK_TYPE:
                String deniedEmail = (String) task.getResult();
                OpenLibre.userProfile.getRequests().remove(deniedEmail);
                listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, OpenLibre.userProfile));
                break;
            case CloudStoreUnlinkTask.TASK_TYPE:
                OpenLibre.userProfile.setType(UserProfile.AccountType.MASTER);
                OpenLibre.userProfile.setMaster(null);
                listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, null));
                break;
            case CloudStoreDetachTask.TASK_TYPE:
                OpenLibre.userProfile.getLinked().remove((String) task.getResult());
                listInvites.setAdapter(new UserLinksRecyclerViewAdapter(this, OpenLibre.userProfile));
                break;
        }
        updateUI(FirebaseAuth.getInstance().getCurrentUser());
    }

    public void onTaskError(CloudStoreTask task){
        rlProgressBar.setVisibility(View.INVISIBLE);
        String error;
        switch (task.getErrorCode()){
            case CloudStoreRequestLinkTask.ERROR_USER_NOT_FOUND:
                error = "User not found";
                break;
            case CloudStoreRequestLinkTask.ERROR_LINK_SELF:
                error = "Can't link to itself";
                break;
            default:
                error = "Link request failed";
                break;
        }
        Toast.makeText(getActivity().getApplicationContext(), error, Toast.LENGTH_LONG).show();
    }

    public void onTaskCancelled(CloudStoreTask taskType) {

    }
}
