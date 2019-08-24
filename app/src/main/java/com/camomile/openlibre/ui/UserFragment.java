package com.camomile.openlibre.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.camomile.openlibre.R;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class UserFragment extends Fragment {

    private static final String LOG_ID = "OpenLibre::" + UserFragment.class.getSimpleName();

    private TextView mLabelUser;
    private AppCompatButton mSignInButton;
    private AppCompatButton mLogoutButton;

    public static UserFragment newInstance() {
        return new UserFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        mLabelUser = (TextView) getView().findViewById(R.id.label_user);
        mSignInButton = (AppCompatButton) getView().findViewById(R.id.button_sign_in);
        mLogoutButton = (AppCompatButton) getView().findViewById(R.id.button_log_out);

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

        updateUI(user);
    }

    private void signOut(FirebaseAuth auth){
        AuthUI.getInstance()
            .signOut(getContext())
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                 @Override
                 public void onComplete(@NonNull Task<Void> task) {
                      Log.d(LOG_ID, "User signed out");
                 }
             });
        updateUI(null);
    }

    private void updateUI(FirebaseUser user)
    {
        if (user == null) {
            mLabelUser.setText(R.string.please_login);
            mSignInButton.setVisibility(View.VISIBLE);
            mLogoutButton.setVisibility(View.GONE);
        }
        else {
            mLabelUser.setText("Name: " + user.getDisplayName() + "\nID: " + user.getUid());
            mSignInButton.setVisibility(View.GONE);
            mLogoutButton.setVisibility(View.VISIBLE);
        }

    }

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
                Log.d(LOG_ID, "Successful sign in. User:" + user.getDisplayName() + " ID:" + user.getUid());
                updateUI(user);
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Log.d(LOG_ID, "Sign in failed");
            }
        }
    }
}
