package com.camomile.openlibre.service;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.camomile.openlibre.OpenLibre;
import com.camomile.openlibre.model.GlucoseData;
import com.camomile.openlibre.model.PredictionData;
import com.camomile.openlibre.model.ReadingData;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendMessageTask extends CloudStoreTask {
    private static final String LOG_ID = "OpenLibre::" + SendMessageTask.class.getSimpleName();
    public static final String TASK_TYPE = "SEND_PUSH";
    public static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";
    public static final String SERVER_KEY = "AAAAvMeXCms:APA91bHDiC3_GCcHPVREk7MAT8S5ByE-j0zG4P6peLliMKknfuVAWy4eM3BqfuLRC6V3cw9vcpMqniWsWt86f0IGYEvJjhHwlTvQYLJLWz4s8duX3cXDdC62hveu9RmRYnCSV8D0oaGM";

    private PushMessage mData;
    private List<String> mTokens;

    public SendMessageTask(PushMessage message, List<String> tokens, ITaskContainer container){
        super(container);
        mTokens = tokens;
        mData = message;
    }

    public JSONObject getMessageJson(PushMessage message, final String token){
        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM HH:mm");
        String title = dateFormat.format(message.getDate());
        String bodyPattern = "Glucose %1$.1f \nPrediction %2$.1f";

        String trend;

        switch (message.getTrend()){
            case TREND_UP: trend = "\u2191"; break;
            case TREND_DOWN: trend = "\u2193"; break;
            case TREND_STABLE: trend = "\u2192"; break;
            case TREND_SLIGHTLY_UP: trend = "\u2197"; break;
            case TREND_SLIGHTLY_DOWN: trend = "\u2198"; break;
            default: trend = "";
        }

        String body = String.format("Glucose %1$.1f ", message.getGlucose()) + trend +
                String.format("\nPrediction %1$.1f", message.getPredictedGlucose());

        try {
            notification.put("to", token);
            notificationBody.put("title", title);
            notificationBody.put("body", body);
            notification.put("notification", notificationBody);
            notification.put("priority", "high");
            return notification;
        }
        catch (JSONException e){
            Log.d(LOG_ID, e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean doWork() {

        for (String token : mTokens) {
            if (token.equals(OpenLibre.deviceAppToken)) continue;

            JSONObject message = getMessageJson(mData, token);
            sendRequest(message);
        }

        return true;
    }

    private void sendRequest(JSONObject message) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                FCM_URL,
                message,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(LOG_ID, "push request completed");
                        Log.d(LOG_ID, response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(LOG_ID, error.getMessage());
                    }
                }
        ){
            @Override
            public Map getHeaders() throws AuthFailureError{
                HashMap headers = new HashMap();
                headers.put("Authorization", "key="+SERVER_KEY);
                headers.put("Content-Type", "application/json");

                return headers;
            }
        };

        Log.v(LOG_ID, "Request queued");
        Log.v(LOG_ID, message.toString());
        OpenLibre.volleyRequestQueue.add(request);
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public Object getResult() {
        return null;
    }
}
