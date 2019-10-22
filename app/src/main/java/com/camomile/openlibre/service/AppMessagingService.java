package com.camomile.openlibre.service;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AppMessagingService extends FirebaseMessagingService {
    private static final String LOG_ID = "AppMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.v(LOG_ID, "MessageId: " + remoteMessage.getMessageId());
        Log.v(LOG_ID, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getNotification() != null)
            Log.v(LOG_ID, "Notification body: " + remoteMessage.getNotification().getBody());
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.v(LOG_ID, "New token: " + token);
    }
}
